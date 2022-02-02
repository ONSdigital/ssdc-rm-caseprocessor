package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ReceiptDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentNextTriggerRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ResponsePeriodRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.SurveyRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.DateOffSet;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.DateUnit;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.ScheduleTemplate;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.ScheduledTaskBuilder;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.Task;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.TemplateType;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.ExportFileRow;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentNextTrigger;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.ResponsePeriod;
import uk.gov.ons.ssdc.common.model.entity.ResponsePeriodState;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;
import uk.gov.ons.ssdc.common.model.entity.Survey;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ScheduledTaskIT {
  private static final String TEST_QID = "123456";
  private static final UUID TEST_UACLINK_ID = UUID.randomUUID();
  private static final String INBOUND_RECEIPT_TOPIC = "event_receipt";
  private static final String START_OF_PERIOD_REMINDER = "CIS_LETTER";
  private static final String PCR_PACKCODE = "CIS_PCR";
  private static final String EQ_PACKCODE = "CIS_EQ";
  private static final String CIS_COMPLETION_FAILURE = "CIS_COMPLETION_FAILURE";

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;

  private static final String EXPORT_FILE_DESTINATION = "TEST_DEST";
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private ScheduledTaskRepository scheduledTaskRepository;
  @Autowired private ResponsePeriodRepository responsePeriodRepository;
  @Autowired private ExportFileTemplateRepository exportFileTemplateRepository;
  @Autowired private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;
  @Autowired private FulfilmentToProcessRepository fulfilmentToProcessRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private SurveyRepository surveyRepository;
  @Autowired private ScheduledTaskBuilder scheduledTaskBuilder;

  @Autowired
  private FulfilmentSurveyExportFileTemplateRepository fulfilmentSurveyExportFileTemplateRepository;

  @Autowired private ExportFileRowRepository exportFileRowRepository;

  @BeforeEach
  public void setUp() {
    clearDown();
  }

  @AfterEach
  public void after() {
    clearDown();
  }

  public void clearDown() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testScheduledTaskRunsIsUpdatedAndPrintFufilmentIsProcessedNoReceiptExpected()
      throws InterruptedException {
    // Given
    Case caze = junkDataHelper.setupJunkCase();

    Survey survey = caze.getCollectionExercise().getSurvey();
    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        addExportFileTemplate(START_OF_PERIOD_REMINDER, new String[] {"__caseref__", "foo"});
    fulfilmentSurveyExportFileTemplate.setSurvey(survey);
    fulfilmentSurveyExportFileTemplateRepository.saveAndFlush(fulfilmentSurveyExportFileTemplate);

    ResponsePeriod responsePeriod = new ResponsePeriod();
    responsePeriod.setCaze(caze);
    responsePeriod.setId(UUID.randomUUID());
    responsePeriod.setResponsePeriodState(ResponsePeriodState.NOT_STARTED);
    responsePeriod.setName("Test response period 1");
    responsePeriodRepository.saveAndFlush(responsePeriod);

    Map<String, String> details =
        Map.of(
            "type",
            ScheduledTaskType.ACTION_WITH_PACKCODE.toString(),
            "packCode",
            START_OF_PERIOD_REMINDER);
    // When - this will save this task to be scheduled,
    ScheduledTask scheduledTask =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.NOT_STARTED, false);

    //
    //    ScheduledTask scheduledTask = new ScheduledTask();
    //    scheduledTask.setId(UUID.randomUUID());
    //    scheduledTask.setRmToActionDate(OffsetDateTime.now());
    //    scheduledTask.setResponsePeriod(responsePeriod);
    //    scheduledTask.setActionState(ScheduledTaskState.NOT_STARTED);
    //    scheduledTask.setReceiptRequiredForCompletion(false);

    //    Map<String, String> details = new HashMap<>();
    //    details.put("ScheduledTaskType", ScheduledTaskType.ACTION_WITH_PACKCODE.toString());
    //    details.put("packCode", packCode);
    //    scheduledTask.setScheduledTaskDetails(details);
    //
    //    scheduledTaskRepository.saveAndFlush(scheduledTask);

    //  What do we expect at the end of N seconds
    // The Scheduled Task should be moved to COMPLETED
    // With a UAC and an Event attached
    // On the ExportFileRows table there should be a sensible new row

    int wait_time_seconds = 10;

    for (int i = 0; i < wait_time_seconds; i++) {
      Thread.sleep(1000);

      if (fulfilmentToProcessRepository.findAll().size() != 0) {
        break;
      }
    }
    // Need to blow up if failed

    // Only do this when the fulfilment is written
    FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
    fulfilmentNextTrigger.setId(UUID.randomUUID());
    fulfilmentNextTrigger.setTriggerDateTime(OffsetDateTime.now());
    fulfilmentNextTriggerRepository.saveAndFlush(fulfilmentNextTrigger);

    // turn into a reliable, wait until row in state etc
    Thread.sleep(5000);

    Optional<ScheduledTask> actualScheduledTaskOpt =
        scheduledTaskRepository.findById(scheduledTask.getId());

    assertThat(actualScheduledTaskOpt).isPresent();
    ScheduledTask actualScheduledTask = actualScheduledTaskOpt.get();
    assertThat(actualScheduledTask.getActionState()).isEqualTo(ScheduledTaskState.COMPLETED);

    Event sentEvent = eventRepository.findById(actualScheduledTask.getSentEventId()).get();
    assertThat(sentEvent).isNotNull();

    assertThat(sentEvent.getType()).isEqualTo(EventType.EXPORT_FILE);
    assertThat(sentEvent.getDescription())
        .isEqualTo("Export file generated with pack code " + START_OF_PERIOD_REMINDER);
    assertThat(sentEvent.getCaze().getId()).isEqualTo(caze.getId());

    assertThat(actualScheduledTask.getUacQidLinkId()).isNull();

    List<ExportFileRow> exportFileRowList = exportFileRowRepository.findAll();
    assertThat(exportFileRowList.size()).isEqualTo(1);
    assertThat(exportFileRowList.get(0).getRow())
        .isEqualTo("\"" + caze.getCaseRef() + "\"|\"bar\"");
  }

  @Test
  public void testSchedulingWithAUACLeavesScheduledTaskInSentState() throws InterruptedException {
    // Given
    Case caze = junkDataHelper.setupJunkCase();
    String packCode = "PACK_CODE_PRINT";

    Survey survey = caze.getCollectionExercise().getSurvey();
    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        addExportFileTemplate(packCode, new String[] {"__caseref__", "foo", "__uac__"});
    fulfilmentSurveyExportFileTemplate.setSurvey(survey);
    fulfilmentSurveyExportFileTemplateRepository.saveAndFlush(fulfilmentSurveyExportFileTemplate);

    ResponsePeriod responsePeriod = new ResponsePeriod();
    responsePeriod.setCaze(caze);
    responsePeriod.setId(UUID.randomUUID());
    responsePeriod.setResponsePeriodState(ResponsePeriodState.NOT_STARTED);
    responsePeriod.setName("Test response period 1");
    responsePeriodRepository.saveAndFlush(responsePeriod);

    ScheduledTask scheduledTask = new ScheduledTask();
    scheduledTask.setId(UUID.randomUUID());
    scheduledTask.setRmToActionDate(OffsetDateTime.now());
    scheduledTask.setResponsePeriod(responsePeriod);
    scheduledTask.setActionState(ScheduledTaskState.NOT_STARTED);
    scheduledTask.setReceiptRequiredForCompletion(true);

    Map<String, String> details = new HashMap<>();
    details.put("type", ScheduledTaskType.ACTION_WITH_PACKCODE.toString());
    details.put("packCode", packCode);
    scheduledTask.setScheduledTaskDetails(details);

    // When - this will save this task to be scheduled,
    scheduledTaskRepository.saveAndFlush(scheduledTask);

    //  What do we expect at the end of N seconds
    // The Scheduled Task should be moved to COMPLETED
    // With a UAC and an Event attached
    // On the ExportFileRows table there should be a sensible new row

    int wait_time_seconds = 10;

    for (int i = 0; i < wait_time_seconds; i++) {
      Thread.sleep(1000);

      if (fulfilmentToProcessRepository.findAll().size() != 0) {
        break;
      }
    }
    // Need to blow up if failed

    // Only do this when the fulfilment is written
    FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
    fulfilmentNextTrigger.setId(UUID.randomUUID());
    fulfilmentNextTrigger.setTriggerDateTime(OffsetDateTime.now());
    fulfilmentNextTriggerRepository.saveAndFlush(fulfilmentNextTrigger);

    // turn into a reliable, wait until row in state etc
    Thread.sleep(5000);

    Optional<ScheduledTask> actualScheduledTaskOpt =
        scheduledTaskRepository.findById(scheduledTask.getId());

    assertThat(actualScheduledTaskOpt).isPresent();
    ScheduledTask actualScheduledTask = actualScheduledTaskOpt.get();
    assertThat(actualScheduledTask.getActionState()).isEqualTo(ScheduledTaskState.SENT);

    Event sentEvent = eventRepository.findById(actualScheduledTask.getSentEventId()).get();
    assertThat(sentEvent).isNotNull();

    assertThat(sentEvent.getType()).isEqualTo(EventType.EXPORT_FILE);
    assertThat(sentEvent.getDescription())
        .isEqualTo("Export file generated with pack code " + packCode);
    assertThat(sentEvent.getCaze().getId()).isEqualTo(caze.getId());

    UacQidLink actualUacQidLink = uacQidLinkRepository.findById(actualScheduledTask.getUacQidLinkId()).get();
    assertThat(actualUacQidLink).isNotNull();

    List<ExportFileRow> exportFileRowList = exportFileRowRepository.findAll();
    assertThat(exportFileRowList.size()).isEqualTo(1);
    assertThat(exportFileRowList.get(0).getRow())
        .isEqualTo(
            "\""
                + caze.getCaseRef()
                + "\"|\"bar\"|\""
                + actualUacQidLink.getUac()
                + "\"");
  }

  @Test
  public void testAScheduledTaskIsReceipted() throws InterruptedException {
    Case caze = junkDataHelper.setupJunkCase();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(TEST_UACLINK_ID);
    uacQidLink.setQid(TEST_QID);
    uacQidLink.setUac("abc");
    uacQidLink.setUacHash("fakeHash");
    uacQidLink.setCaze(caze);
    uacQidLink.setActive(true);
    uacQidLink.setReceiptReceived(false);
    uacQidLink.setEqLaunched(false);
    uacQidLink.setCollectionInstrumentUrl("dummyUrl");
    uacQidLinkRepository.saveAndFlush(uacQidLink);

    ResponsePeriod responsePeriod = new ResponsePeriod();
    responsePeriod.setCaze(caze);
    responsePeriod.setId(UUID.randomUUID());
    responsePeriod.setResponsePeriodState(ResponsePeriodState.NOT_STARTED);
    responsePeriod.setName("Test response period 1");
    responsePeriodRepository.saveAndFlush(responsePeriod);

    ScheduledTask scheduledTask = new ScheduledTask();
    scheduledTask.setId(UUID.randomUUID());
    scheduledTask.setActionState(ScheduledTaskState.SENT);
    scheduledTask.setUacQidLinkId(uacQidLink.getId());
    scheduledTask.setReceiptRequiredForCompletion(true);
    scheduledTask.setResponsePeriod(responsePeriod);
    scheduledTask.setRmToActionDate(OffsetDateTime.now().minusDays(1L));
    scheduledTaskRepository.saveAndFlush(scheduledTask);

    uacQidLink.setScheduledTaskId(scheduledTask.getId());
    uacQidLinkRepository.saveAndFlush(uacQidLink);

    try (QueueSpy<EventDTO> outboundUacQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      ReceiptDTO receiptDTO = new ReceiptDTO();
      receiptDTO.setQid(TEST_QID);
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setReceipt(receiptDTO);
      EventDTO event = new EventDTO();
      event.setPayload(payloadDTO);

      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
      eventHeader.setTopic(INBOUND_RECEIPT_TOPIC);
      junkDataHelper.junkify(eventHeader);
      event.setHeader(eventHeader);

      pubsubHelper.sendMessageToSharedProject(INBOUND_RECEIPT_TOPIC, event);

      //  THEN
      EventDTO uacUpdatedEvent = outboundUacQueueSpy.checkExpectedMessageReceived();
      UacUpdateDTO emittedUac = uacUpdatedEvent.getPayload().getUacUpdate();
      Assertions.assertThat(emittedUac.isActive()).isFalse();
      Assertions.assertThat(emittedUac.isReceiptReceived()).isTrue();

      List<Event> storedEvents = eventRepository.findAll();
      Assertions.assertThat(storedEvents.size()).isEqualTo(1);
      Assertions.assertThat(storedEvents.get(0).getUacQidLink().getId()).isEqualTo(TEST_UACLINK_ID);
      Assertions.assertThat(storedEvents.get(0).getType()).isEqualTo(EventType.RECEIPT);

      Optional<ScheduledTask> actualScheduledTaskOpt =
          scheduledTaskRepository.findById(scheduledTask.getId());

      assertThat(actualScheduledTaskOpt).isPresent();
      ScheduledTask actualScheduledTask = actualScheduledTaskOpt.get();
      assertThat(actualScheduledTask.getActionState()).isEqualTo(ScheduledTaskState.COMPLETED);
    }
  }

  @Test
  public void testNoneCompletionFindsAllOK() throws InterruptedException {
    /*
    In this test case we want to have 3 scheduledTasks
      1. A scheduledTask for ReminderSent out: COMPLETED
      2. A scheduledTask for PCR out: COMPLETED
      3. A scheduledTask for EQ out: COMPLETED
      4. A scheduledTask for CheckForCompletion: NOT_STARTED, scheduled for now!
    */

    // Given
    Case caze = junkDataHelper.setupJunkCase();
    String packCode = "CIS_COMPLETION_FAILURE";

    Survey survey = caze.getCollectionExercise().getSurvey();
    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        addExportFileTemplate(packCode, new String[] {"__caseref__", "foo"});
    fulfilmentSurveyExportFileTemplate.setSurvey(survey);
    fulfilmentSurveyExportFileTemplateRepository.saveAndFlush(fulfilmentSurveyExportFileTemplate);

    ResponsePeriod responsePeriod = new ResponsePeriod();
    responsePeriod.setCaze(caze);
    responsePeriod.setId(UUID.randomUUID());
    responsePeriod.setResponsePeriodState(ResponsePeriodState.NOT_STARTED);
    responsePeriod.setName("Test response period 1");
    responsePeriodRepository.saveAndFlush(responsePeriod);

    Map<String, String> details =
        Map.of(
            "type",
            ScheduledTaskType.ACTION_WITH_PACKCODE.toString(),
            "packCode",
            START_OF_PERIOD_REMINDER);
    ScheduledTask reminderLetter =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.COMPLETED, false);

    details =
        Map.of("type", ScheduledTaskType.ACTION_WITH_PACKCODE.toString(), "packCode", PCR_PACKCODE);
    ScheduledTask pcr =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.COMPLETED, true);

    details =
        Map.of("type", ScheduledTaskType.ACTION_WITH_PACKCODE.toString(), "packCode", EQ_PACKCODE);
    ScheduledTask eq =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.COMPLETED, true);

    /*
     Now the task to check if Completed In Time
     In CIS this might be closed by INCENTIVE, but maybe not depending on timing
     Might be receipted after failed INCENTIVE run, so it's not a failure

     This task should run, find that all of the above are ok.

     Not best place to record but:
        How do we shut out a responsePeriod and by extension it's scheduledTasks.

        A checkForCompletion could do this if it's a 'fail' e.g. we send out a letter, but not a 'pass' e.g. no letter?
        An Incentive passed could do this? but not a failed?

        Maybe another flag, or something in the details?

        If this complete/failed then lock out responsePeriod
    */

    // WHEN
    // Complex Detail thinking here, going to hardcode something for CIS instead for now
    //    details =
    //            Map.of("ScheduledTaskType", ScheduledTaskType.ASSESS_SOME_SPEL.toString(),
    //                    "test", "All_ACTION_WITH_PACKCODE_complete",
    //                    "pass", "{\"state\":\"COMPLETED\", \"action\":\"\"}",
    //                    "fail", "");
    //    For CIS and others it may just be this simple
    // And yes this isn't SPEL, nor is the JSON above.
    details =
        Map.of("type", ScheduledTaskType.COMPLETION.toString(), "packCode", CIS_COMPLETION_FAILURE);
    ScheduledTask completionCheck =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.NOT_STARTED, false);

    int wait_time_seconds = 10;

    for (int i = 0; i < wait_time_seconds; i++) {
      Thread.sleep(1000);

      if (fulfilmentToProcessRepository.findAll().size() != 0) {
        fail("Should not create a Fulfliment");
      }
    }

    Optional<ScheduledTask> actualScheduledTaskOpt =
        scheduledTaskRepository.findById(completionCheck.getId());

    assertThat(actualScheduledTaskOpt).isPresent();
    ScheduledTask actualScheduledTask = actualScheduledTaskOpt.get();
    assertThat(actualScheduledTask.getActionState()).isEqualTo(ScheduledTaskState.COMPLETED);

    assertThat(actualScheduledTask.getSentEventId()).isNull();
  }

  @Test
  public void testNoneCompletionFindsFailure() throws InterruptedException {
    /*
    In this test case we want to have 3 scheduledTasks
      1. A scheduledTask for ReminderSent out: COMPLETED
      2. A scheduledTask for PCR out: COMPLETED
      3. A scheduledTask for EQ out: COMPLETED
      4. A scheduledTask for CheckForCompletion: NOT_STARTED, scheduled for now!
    */

    // Given
    Case caze = junkDataHelper.setupJunkCase();
    String packCode = "CIS_COMPLETION_FAILURE";

    Survey survey = caze.getCollectionExercise().getSurvey();
    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        addExportFileTemplate(packCode, new String[] {"__caseref__", "foo"});
    fulfilmentSurveyExportFileTemplate.setSurvey(survey);
    fulfilmentSurveyExportFileTemplateRepository.saveAndFlush(fulfilmentSurveyExportFileTemplate);

    ResponsePeriod responsePeriod = new ResponsePeriod();
    responsePeriod.setCaze(caze);
    responsePeriod.setId(UUID.randomUUID());
    responsePeriod.setResponsePeriodState(ResponsePeriodState.NOT_STARTED);
    responsePeriod.setName("Test response period 1");
    responsePeriodRepository.saveAndFlush(responsePeriod);

    Map<String, String> details =
        Map.of(
            "type",
            ScheduledTaskType.ACTION_WITH_PACKCODE.toString(),
            "packCode",
            START_OF_PERIOD_REMINDER);
    ScheduledTask reminderLetter =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.COMPLETED, false);

    details =
        Map.of("type", ScheduledTaskType.ACTION_WITH_PACKCODE.toString(), "packCode", PCR_PACKCODE);
    ScheduledTask pcr =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.COMPLETED, true);

    details =
        Map.of("type", ScheduledTaskType.ACTION_WITH_PACKCODE.toString(), "packCode", EQ_PACKCODE);
    ScheduledTask incompleteEQ =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.SENT, true);

    details =
        Map.of("type", ScheduledTaskType.COMPLETION.toString(), "packCode", CIS_COMPLETION_FAILURE);
    ScheduledTask completionCheck =
        addScheduledTask(responsePeriod, details, ScheduledTaskState.NOT_STARTED, false);

    int wait_time_seconds = 10;

    for (int i = 0; i < wait_time_seconds; i++) {
      Thread.sleep(1000);

      if (fulfilmentToProcessRepository.findAll().size() != 0) {
        break;
      }
    }

    // blow up on failure above -- make nice retry function db polling function, if we don't have
    // one

    // Only do this when the fulfilment is written
    FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
    fulfilmentNextTrigger.setId(UUID.randomUUID());
    fulfilmentNextTrigger.setTriggerDateTime(OffsetDateTime.now());
    fulfilmentNextTriggerRepository.saveAndFlush(fulfilmentNextTrigger);

    // turn into a reliable, wait until row in state etc
    Thread.sleep(5000);

    Optional<ScheduledTask> actualScheduledTaskOpt =
        scheduledTaskRepository.findById(completionCheck.getId());

    assertThat(actualScheduledTaskOpt).isPresent();
    ScheduledTask actualScheduledTask = actualScheduledTaskOpt.get();
    assertThat(actualScheduledTask.getActionState())
        .isEqualTo(ScheduledTaskState.NOT_COMPLETED_WITHIN_PERIOD);

    assertThat(actualScheduledTask.getSentEventId()).isNotNull();
  }

  @Test
  public void turnTemplateIntoScheduledTasksOnCaseCreation() throws InterruptedException, JsonProcessingException {
    Case caze = junkDataHelper.setupJunkCase();
    Survey survey = caze.getCollectionExercise().getSurvey();

    // Maybe build in caseprocessor for now, then move to DLL
    ScheduleTemplate scheduleTemplate = new ScheduleTemplate();
    scheduleTemplate.setType(TemplateType.REPEAT);
    scheduleTemplate.setTaskSpacing(
        new DateOffSet[] {
          new DateOffSet(DateUnit.WEEK, 1),
          new DateOffSet(DateUnit.WEEK, 1),
          new DateOffSet(DateUnit.WEEK, 1),
          new DateOffSet(DateUnit.WEEK, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1),
          new DateOffSet(DateUnit.MONTH, 1)
        });

    scheduleTemplate.setScheduleFromCreate(true);
    scheduleTemplate.setStartDate(null);

    scheduleTemplate.setTasks(
        new Task[] {
          new Task(
              "Start Of Period Letter",
              ScheduledTaskType.ACTION_WITH_PACKCODE,
              "CIS_REMINDERR",
              false,
              new DateOffSet(DateUnit.DAY, 0)),
          new Task(
              "PCR ExportFile",
              ScheduledTaskType.ACTION_WITH_PACKCODE,
              "CIS_PCR",
              false,
              new DateOffSet(DateUnit.DAY, 7)),
          new Task(
              "EQ",
              ScheduledTaskType.ACTION_WITH_PACKCODE,
              "CIS_EQ",
              false,
              new DateOffSet(DateUnit.DAY, 7)),
          new Task(
              "Incentive",
              ScheduledTaskType.INCENTIVE,
              "CIS_INCENTIVE",
              false,
              new DateOffSet(DateUnit.DAY, 10))
        });

    ObjectMapper objectMapper = new ObjectMapper();
    String scheduledTemplateJSON = objectMapper.writeValueAsString(scheduleTemplate);

    survey.setScheduleTemplate(scheduledTemplateJSON);
    surveyRepository.saveAndFlush(survey);

    // Set one up, but in the future, we don't want them to fire for this particular test
    //When
    OffsetDateTime actualStartDate = OffsetDateTime.now().plusDays(1);
    List<ResponsePeriod> responsePeriodList = scheduledTaskBuilder.buildResponsePeriodAndScheduledTasks(caze,
            actualStartDate );

    // Yes... as those paying attention will have noticed, this is not really a proper IT,
    // I'd expect it to create the Caze with an Event, and then check it.  But it's part of a spike,
    // nice for it to exercise the database correctly too, rather than mock the hell out of everything

    // Then
    // What are we expecting?
    // X responsePeriods, each with N Scheduled Tasks

    List<ResponsePeriod> actualResponsePeriodList = responsePeriodRepository.findAll();
    assertThat(actualResponsePeriodList.size()).isEqualTo(scheduleTemplate.getTaskSpacing().length);
    
    List<ScheduledTask> actualScheduledTasksAll = scheduledTaskRepository.findAll();
    assertThat(actualScheduledTasksAll.size()).isEqualTo(scheduleTemplate.getTaskSpacing().length * scheduleTemplate.getTasks().length);


//    for(ResponsePeriod responsePeriod : actualResponsePeriodList) {
//      ScheduledTask reminderLetter = responsePeriod.getScheduledTasks().get(0);
//      assertThat(reminderLetter.getTaskName()).isEqualTo("Start Of Period Letter");
//    }
  }

  private ScheduledTask addScheduledTask(
      ResponsePeriod responsePeriod,
      Map<String, String> details,
      ScheduledTaskState scheduledTaskState,
      boolean receiptRequired) {
    ScheduledTask scheduledTask = new ScheduledTask();
    scheduledTask.setId(UUID.randomUUID());
    scheduledTask.setRmToActionDate(OffsetDateTime.now());
    scheduledTask.setResponsePeriod(responsePeriod);
    scheduledTask.setActionState(scheduledTaskState);
    scheduledTask.setReceiptRequiredForCompletion(receiptRequired);
    scheduledTask.setScheduledTaskDetails(details);
    return scheduledTaskRepository.saveAndFlush(scheduledTask);
  }

  private FulfilmentSurveyExportFileTemplate addExportFileTemplate(
      String packCode, String[] template) {
    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setPackCode(packCode);
    exportFileTemplate.setExportFileDestination(EXPORT_FILE_DESTINATION);
    exportFileTemplate.setTemplate(template);
    exportFileTemplate.setDescription("Test description");
    exportFileTemplateRepository.saveAndFlush(exportFileTemplate);

    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        new FulfilmentSurveyExportFileTemplate();
    fulfilmentSurveyExportFileTemplate.setExportFileTemplate(exportFileTemplate);
    fulfilmentSurveyExportFileTemplate.setId(UUID.randomUUID());

    return fulfilmentSurveyExportFileTemplate;
  }
}
