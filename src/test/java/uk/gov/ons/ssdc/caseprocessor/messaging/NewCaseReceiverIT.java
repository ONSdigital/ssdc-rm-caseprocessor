package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.ScheduleTaskHelper.createOneTaskSimpleScheduleTemplate;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.NEW_CASE_TOPIC;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.NewCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentNextTriggerRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.SurveyRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.ExportFileRow;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentNextTrigger;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.Survey;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;
import uk.gov.ons.ssdc.common.validation.MandatoryRule;
import uk.gov.ons.ssdc.common.validation.Rule;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class NewCaseReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String TEST_PACK_CODE = "Test-Pack-Code";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired
  private PubsubHelper pubsubHelper;
  @Autowired
  private DeleteDataHelper deleteDataHelper;
  @Autowired
  private JunkDataHelper junkDataHelper;

  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private CaseRepository caseRepository;

  @Autowired
  private ScheduledTaskRepository scheduledTaskRepository;
  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private ExportFileTemplateRepository exportFileTemplateRepository;

  @Autowired
  private ExportFileRowRepository exportFileRowRepository;

  @Autowired
  private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;

  @Autowired
  private FulfilmentSurveyExportFileTemplateRepository fulfilmentSurveyExportFileTemplateRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testNewCaseLoaded() throws InterruptedException {
    try (QueueSpy<EventDTO> outboundCaseQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_CASE_SUBSCRIPTION, EventDTO.class)) {

      // GIVEN
      EventDTO event = new EventDTO();
      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
      eventHeader.setTopic(NEW_CASE_TOPIC);
      junkDataHelper.junkify(eventHeader);
      event.setHeader(eventHeader);

      CollectionExercise collectionExercise = junkDataHelper.setupJunkCollex();

      Map<String, String> sample = new HashMap<>();
      sample.put("Junk", "YesYouCan");

      Map<String, String> sampleSensitive = new HashMap<>();
      sampleSensitive.put("SensitiveJunk", "02071234567");

      PayloadDTO payloadDTO = new PayloadDTO();
      NewCase newCase = new NewCase();
      newCase.setCaseId(TEST_CASE_ID);
      newCase.setCollectionExerciseId(collectionExercise.getId());
      newCase.setSample(sample);
      newCase.setSampleSensitive(sampleSensitive);
      payloadDTO.setNewCase(newCase);
      event.setPayload(payloadDTO);

      pubsubHelper.sendMessageToSharedProject(NEW_CASE_TOPIC, event);

      //  THEN
      EventDTO actualEvent = outboundCaseQueueSpy.checkExpectedMessageReceived();

      CaseUpdateDTO emittedCase = actualEvent.getPayload().getCaseUpdate();
      Assertions.assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);
      Assertions.assertThat(emittedCase.getCollectionExerciseId())
          .isEqualTo(collectionExercise.getId());
      Assertions.assertThat(emittedCase.getSurveyId())
          .isEqualTo(collectionExercise.getSurvey().getId());

      Case actualCase = caseRepository.findById(TEST_CASE_ID).get();

      assertThat(actualCase.getId()).isEqualTo(TEST_CASE_ID);
      assertThat(actualCase.getCollectionExercise().getId()).isEqualTo(collectionExercise.getId());
      assertThat(actualCase.getSample()).isEqualTo(sample);
      assertThat(actualCase.getSampleSensitive()).isEqualTo(sampleSensitive);

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isEqualTo(1);
      assertThat(events.get(0).getType()).isEqualTo(EventType.NEW_CASE);
      assertThat(events.get(0).getPayload()).contains("{\"SensitiveJunk\": \"REDACTED\"}");
    }
  }

  @Test
  public void testNewCaseLoadedWithScheduleSet() throws InterruptedException {
    try (QueueSpy<EventDTO> outboundCaseQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_CASE_SUBSCRIPTION, EventDTO.class)) {

      // GIVEN
      EventDTO event = new EventDTO();
      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
      eventHeader.setTopic(NEW_CASE_TOPIC);
      junkDataHelper.junkify(eventHeader);
      event.setHeader(eventHeader);

      CollectionExercise collectionExercise = junkDataHelper.setupJunkCollex();
      // Setting up schedules

      ExportFileTemplate exportFileTemplate = addExportFileTemplate(TEST_PACK_CODE);
      allowPackCodeOnSurvey(TEST_PACK_CODE, collectionExercise.getSurvey(), exportFileTemplate);

      ScheduleTemplate scheduleTemplate =
          createOneTaskSimpleScheduleTemplate(ChronoUnit.SECONDS, 0);

      Survey survey = collectionExercise.getSurvey();

      survey.setScheduleTemplate(scheduleTemplate);

      survey.setSampleValidationRules(
          new ColumnValidator[]{
              new ColumnValidator("ADDRESS_LINE1", false, new Rule[]{new MandatoryRule()}),
              new ColumnValidator("POSTCODE", false, new Rule[]{new MandatoryRule()}),
              new ColumnValidator("SensitiveJunk", true, new Rule[] {new MandatoryRule()})
          });

      surveyRepository.saveAndFlush(survey);

      Map<String, String> sample = new HashMap<>();
      sample.put("ADDRESS_LINE1", "666 Fake Street");
      sample.put("POSTCODE", "PO57 C0D");

      Map<String, String> sampleSensitive = new HashMap<>();
      sampleSensitive.put("SensitiveJunk", "02071234567");

      PayloadDTO payloadDTO = new PayloadDTO();
      NewCase newCase = new NewCase();
      newCase.setCaseId(TEST_CASE_ID);
      newCase.setCollectionExerciseId(collectionExercise.getId());
      newCase.setSample(sample);
      newCase.setSampleSensitive(sampleSensitive);
      payloadDTO.setNewCase(newCase);
      event.setPayload(payloadDTO);

      pubsubHelper.sendMessageToSharedProject(NEW_CASE_TOPIC, event);

      //  THEN
      EventDTO actualEvent = outboundCaseQueueSpy.checkExpectedMessageReceived();

      CaseUpdateDTO emittedCase = actualEvent.getPayload().getCaseUpdate();
      Assertions.assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);
      Assertions.assertThat(emittedCase.getCollectionExerciseId())
          .isEqualTo(collectionExercise.getId());
      Assertions.assertThat(emittedCase.getSurveyId())
          .isEqualTo(collectionExercise.getSurvey().getId());

      Case actualCase = caseRepository.findById(TEST_CASE_ID).get();

      assertThat(actualCase.getId()).isEqualTo(TEST_CASE_ID);
      assertThat(actualCase.getCollectionExercise().getId()).isEqualTo(collectionExercise.getId());
      assertThat(actualCase.getSample()).isEqualTo(sample);
      assertThat(actualCase.getSampleSensitive()).isEqualTo(sampleSensitive);

      assertThat(actualCase.getSchedule()).isNotNull();
      ArrayList<Map> actualSchedule = (ArrayList<Map>) actualCase.getSchedule();
      assertThat(actualSchedule.size()).isEqualTo(1);

      assertThat(actualSchedule.get(0).get("name")).isEqualTo("Task Group 1");

      ArrayList<Map> actualScheduleTasks =
          (ArrayList<Map>) actualSchedule.get(0).get("scheduledTasks");
      assertThat(actualScheduleTasks.size()).isEqualTo(1);

      Map task = actualScheduleTasks.get(0);
      assertThat(task.get("name")).isEqualTo("Task 1");

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isEqualTo(1);
      assertThat(events.get(0).getType()).isEqualTo(EventType.NEW_CASE);
      assertThat(events.get(0).getPayload()).contains("{\"SensitiveJunk\": \"REDACTED\"}");

      // TODO: Alter

      Thread.sleep(5000);
      assertThat(scheduledTaskRepository.findAll().size()).isEqualTo(0);

      FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
      fulfilmentNextTrigger.setId(UUID.randomUUID());
      fulfilmentNextTrigger.setTriggerDateTime(OffsetDateTime.now());
      fulfilmentNextTriggerRepository.saveAndFlush(fulfilmentNextTrigger);

      Thread.sleep(5000);

      List<ExportFileRow> exportFileRows = exportFileRowRepository.findAll();
      ExportFileRow exportFileRow = exportFileRows.get(0);

      // Then
      Assertions.assertThat(exportFileRow).isNotNull();
      Assertions.assertThat(exportFileRow.getBatchQuantity()).isEqualTo(1);
      Assertions.assertThat(exportFileRow.getPackCode()).isEqualTo(TEST_PACK_CODE);
      Assertions.assertThat(exportFileRow.getExportFileDestination()).isEqualTo("SUPPLIER_A");
      Assertions.assertThat(exportFileRow.getRow()).isEqualTo("\"666 Fake Street\"|\"PO57 C0D\"");
    }
  }

  private void allowPackCodeOnSurvey(
      String packCode, Survey survey, ExportFileTemplate exportFileTemplate) {
    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        new FulfilmentSurveyExportFileTemplate();
    fulfilmentSurveyExportFileTemplate.setSurvey(survey);
    fulfilmentSurveyExportFileTemplate.setId(UUID.randomUUID());
    fulfilmentSurveyExportFileTemplate.setExportFileTemplate(exportFileTemplate);

    fulfilmentSurveyExportFileTemplateRepository.saveAndFlush(fulfilmentSurveyExportFileTemplate);
  }

  private ExportFileTemplate addExportFileTemplate(String packCode) {
    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setPackCode(packCode);
    exportFileTemplate.setTemplate(new String[]{"ADDRESS_LINE1", "POSTCODE"});
    exportFileTemplate.setExportFileDestination("SUPPLIER_A");
    exportFileTemplate.setMetadata("{\"foo\": \"bar\"}");
    exportFileTemplate.setDescription("test");

    exportFileTemplateRepository.saveAndFlush(exportFileTemplate);

    return exportFileTemplate;
  }
}
