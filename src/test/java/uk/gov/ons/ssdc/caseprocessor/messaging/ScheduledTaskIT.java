package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentNextTriggerRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ResponsePeriodRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
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

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ScheduledTaskIT {
  private static final String EXPORT_FILE_DESTINATION = "TEST_DEST";
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private ScheduledTaskRepository scheduledTaskRepository;
  @Autowired private ResponsePeriodRepository responsePeriodRepository;
  @Autowired private ExportFileTemplateRepository exportFileTemplateRepository;
  @Autowired private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;
  @Autowired private FulfilmentToProcessRepository fulfilmentToProcessRepository;

  @Autowired
  private FulfilmentSurveyExportFileTemplateRepository fulfilmentSurveyExportFileTemplateRepository;

  @Autowired private ExportFileRowRepository exportFileRowRepository;

  @BeforeEach
  public void setUp() {
    deleteDataHelper.deleteAllData();
  }

  @AfterEach
  public void after() {
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testScheduledTaskRunsIsUpdatedAndPrintFufilmentIsProcessedNoReceiptExpected()
      throws InterruptedException {
    // Given
    Case caze = junkDataHelper.setupJunkCase();
    String packCode = "PACK_CODE_PRINT";

    Survey survey = caze.getCollectionExercise().getSurvey();
    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        addExportFileTemplate(packCode, new String [] {"__caseref__", "foo"});
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
    scheduledTask.setReceiptRequiredForCompletion(false);

    Map<String, String> details = new HashMap<>();
    details.put("ScheduledTaskType", ScheduledTaskType.ACTION_WITH_PACKCODE.toString());
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
    assertThat(actualScheduledTask.getActionState()).isEqualTo(ScheduledTaskState.COMPLETED);

    Event sentEvent = actualScheduledTask.getSentEvent();
    assertThat(sentEvent).isNotNull();

    assertThat(sentEvent.getType()).isEqualTo(EventType.EXPORT_FILE);
    assertThat(sentEvent.getDescription()).isEqualTo("Export file generated with pack code " + packCode);
    assertThat(sentEvent.getCaze().getId()).isEqualTo(caze.getId());

    assertThat(actualScheduledTask.getUacQidLink()).isNull();

    List<ExportFileRow> exportFileRowList = exportFileRowRepository.findAll();
    assertThat(exportFileRowList.size()).isEqualTo(1);
    assertThat(exportFileRowList.get(0).getRow()).isEqualTo( "\"" +  caze.getCaseRef() + "\"|\"bar\"");
}

  @Test
  public void testSchedulingWithAUACLeavesScheduledTaskInSentState()
          throws InterruptedException {
    // Given
    Case caze = junkDataHelper.setupJunkCase();
    String packCode = "PACK_CODE_PRINT";

    Survey survey = caze.getCollectionExercise().getSurvey();
    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
            addExportFileTemplate(packCode, new String [] {"__caseref__", "foo", "__uac__"});
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
    details.put("ScheduledTaskType", ScheduledTaskType.ACTION_WITH_PACKCODE.toString());
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

    Event sentEvent = actualScheduledTask.getSentEvent();
    assertThat(sentEvent).isNotNull();

    assertThat(sentEvent.getType()).isEqualTo(EventType.EXPORT_FILE);
    assertThat(sentEvent.getDescription()).isEqualTo("Export file generated with pack code " + packCode);
    assertThat(sentEvent.getCaze().getId()).isEqualTo(caze.getId());

    assertThat(actualScheduledTask.getUacQidLink()).isNull();

    List<ExportFileRow> exportFileRowList = exportFileRowRepository.findAll();
    assertThat(exportFileRowList.size()).isEqualTo(1);
    assertThat(exportFileRowList.get(0).getRow()).isEqualTo( "\"" +  caze.getCaseRef() + "\"|\"bar\"|\"" +
            actualScheduledTask.getUacQidLink().getUac() + "\"");
  }



  private FulfilmentSurveyExportFileTemplate addExportFileTemplate(String packCode, String [] template) {
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
