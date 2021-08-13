package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintFulfilmentDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentNextTrigger;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentSurveyPrintTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.entity.PrintTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Survey;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentNextTriggerRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyPrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.SurveyRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class FulfilmentIT {
  private static final String OUTBOUND_PRINTER_SUBSCRIPTION =
      "rm-internal-print-row_print-file-service";
  private static final String FULFILMENT_TOPIC = "event_print-fulfilment";

  private static final String PACK_CODE = "test-pack-code";
  private static final String PRINT_SUPPLIER = "FOOBAR_PRINT_SUPPLIER";

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Value("${queueconfig.print-topic}")
  private String printTopic;

  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private SurveyRepository surveyRepository;
  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;
  @Autowired private PrintTemplateRepository printTemplateRepository;

  @Autowired
  private FulfilmentSurveyPrintTemplateRepository fulfilmentSurveyPrintTemplateRepository;

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_PRINTER_SUBSCRIPTION, printTopic);
    pubsubHelper.purgeMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testFulfilmentTrigger() throws Exception {
    try (QueueSpy<PrintRow> printerQueue =
            pubsubHelper.listen(OUTBOUND_PRINTER_SUBSCRIPTION, PrintRow.class);
        QueueSpy<ResponseManagementEvent> outboundUacQueue =
            pubsubHelper.listen(OUTBOUND_UAC_SUBSCRIPTION, ResponseManagementEvent.class)) {
      // Given
      PrintTemplate printTemplate = new PrintTemplate();
      printTemplate.setPackCode(PACK_CODE);
      printTemplate.setPrintSupplier(PRINT_SUPPLIER);
      printTemplate.setTemplate(new String[] {"__caseref__", "foo", "__uac__"});
      printTemplateRepository.saveAndFlush(printTemplate);

      Survey survey = setUpSurvey();

      FulfilmentSurveyPrintTemplate fulfilmentSurveyPrintTemplate =
          new FulfilmentSurveyPrintTemplate();
      fulfilmentSurveyPrintTemplate.setId(UUID.randomUUID());
      fulfilmentSurveyPrintTemplate.setSurvey(survey);
      fulfilmentSurveyPrintTemplate.setPrintTemplate(printTemplate);
      fulfilmentSurveyPrintTemplateRepository.saveAndFlush(fulfilmentSurveyPrintTemplate);

      CollectionExercise collectionExercise = setUpCollectionExercise(survey);
      Case caze = setUpCase(collectionExercise);

      // When
      PrintFulfilmentDTO fulfilment = new PrintFulfilmentDTO();
      fulfilment.setCaseId(caze.getId());
      fulfilment.setPackCode(PACK_CODE);

      PayloadDTO payload = new PayloadDTO();
      payload.setPrintFulfilment(fulfilment);

      ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
      responseManagementEvent.setPayload(payload);

      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.PRINT_FULFILMENT);
      eventDTO.setSource("RH");
      eventDTO.setDateTime(OffsetDateTime.now());
      eventDTO.setChannel("RH");
      eventDTO.setTransactionId(UUID.randomUUID());
      responseManagementEvent.setEvent(eventDTO);

      pubsubHelper.sendMessage(FULFILMENT_TOPIC, responseManagementEvent);

      Thread.sleep(3000);

      FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
      fulfilmentNextTrigger.setId(UUID.randomUUID());
      fulfilmentNextTrigger.setTriggerDateTime(OffsetDateTime.now());
      fulfilmentNextTriggerRepository.saveAndFlush(fulfilmentNextTrigger);

      PrintRow printRow = printerQueue.getQueue().poll(20, TimeUnit.SECONDS);
      ResponseManagementEvent rme = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(printRow).isNotNull();
      assertThat(printRow.getBatchQuantity()).isEqualTo(1);
      assertThat(printRow.getPackCode()).isEqualTo(PACK_CODE);
      assertThat(printRow.getPrintSupplier()).isEqualTo(PRINT_SUPPLIER);
      assertThat(printRow.getRow()).startsWith("\"123\"|\"bar\"|\"");

      assertThat(rme).isNotNull();
      assertThat(rme.getEvent().getType()).isEqualTo(EventTypeDTO.UAC_UPDATE);
      assertThat(rme.getPayload().getUacUpdate().getCaseId()).isEqualTo(caze.getId());
    }
  }

  private Survey setUpSurvey() {
    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    survey.setSampleSeparator(',');
    return surveyRepository.saveAndFlush(survey);
  }

  private CollectionExercise setUpCollectionExercise(Survey survey) {
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    collectionExercise.setSurvey(survey);
    return collectionExerciseRepository.saveAndFlush(collectionExercise);
  }

  private Case setUpCase(CollectionExercise collectionExercise) {
    Case randomCase = new Case();
    randomCase.setId(UUID.randomUUID());
    randomCase.setCaseRef(123L);
    randomCase.setCollectionExercise(collectionExercise);
    randomCase.setSample(Map.of("foo", "bar"));
    return caseRepository.saveAndFlush(randomCase);
  }
}
