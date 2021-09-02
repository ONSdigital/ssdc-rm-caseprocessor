package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintFulfilmentDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentNextTriggerRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyPrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentNextTrigger;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyPrintTemplate;
import uk.gov.ons.ssdc.common.model.entity.PrintTemplate;

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
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;
  @Autowired private PrintTemplateRepository printTemplateRepository;

  @Autowired
  private FulfilmentSurveyPrintTemplateRepository fulfilmentSurveyPrintTemplateRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_PRINTER_SUBSCRIPTION, printTopic);
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testFulfilmentTrigger() throws Exception {
    try (QueueSpy<PrintRow> printerQueue =
            pubsubHelper.listen(OUTBOUND_PRINTER_SUBSCRIPTION, PrintRow.class);
        QueueSpy<EventDTO> outboundUacQueue =
            pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      // Given
      PrintTemplate printTemplate = new PrintTemplate();
      printTemplate.setPackCode(PACK_CODE);
      printTemplate.setPrintSupplier(PRINT_SUPPLIER);
      printTemplate.setTemplate(new String[] {"__caseref__", "foo", "__uac__"});
      printTemplateRepository.saveAndFlush(printTemplate);

      Case caze = junkDataHelper.setupJunkCase();

      FulfilmentSurveyPrintTemplate fulfilmentSurveyPrintTemplate =
          new FulfilmentSurveyPrintTemplate();
      fulfilmentSurveyPrintTemplate.setId(UUID.randomUUID());
      fulfilmentSurveyPrintTemplate.setSurvey(caze.getCollectionExercise().getSurvey());
      fulfilmentSurveyPrintTemplate.setPrintTemplate(printTemplate);
      fulfilmentSurveyPrintTemplateRepository.saveAndFlush(fulfilmentSurveyPrintTemplate);

      // When
      PrintFulfilmentDTO fulfilment = new PrintFulfilmentDTO();
      fulfilment.setCaseId(caze.getId());
      fulfilment.setPackCode(PACK_CODE);

      PayloadDTO payload = new PayloadDTO();
      payload.setPrintFulfilment(fulfilment);

      EventDTO event = new EventDTO();
      event.setPayload(payload);

      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setVersion(EVENT_SCHEMA_VERSION);
      eventHeader.setTopic(FULFILMENT_TOPIC);
      junkDataHelper.junkify(eventHeader);
      event.setHeader(eventHeader);

      pubsubHelper.sendMessageToSharedProject(FULFILMENT_TOPIC, event);

      Thread.sleep(3000);

      FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
      fulfilmentNextTrigger.setId(UUID.randomUUID());
      fulfilmentNextTrigger.setTriggerDateTime(OffsetDateTime.now());
      fulfilmentNextTriggerRepository.saveAndFlush(fulfilmentNextTrigger);

      PrintRow printRow = printerQueue.getQueue().poll(20, TimeUnit.SECONDS);
      EventDTO rme = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(printRow).isNotNull();
      assertThat(printRow.getBatchQuantity()).isEqualTo(1);
      assertThat(printRow.getPackCode()).isEqualTo(PACK_CODE);
      assertThat(printRow.getPrintSupplier()).isEqualTo(PRINT_SUPPLIER);
      assertThat(printRow.getRow()).startsWith("\"" + caze.getCaseRef() + "\"|\"bar\"|\"");

      assertThat(rme).isNotNull();
      assertThat(rme.getHeader().getTopic()).isEqualTo(uacUpdateTopic);
      assertThat(rme.getPayload().getUacUpdate().getCaseId()).isEqualTo(caze.getId());
    }
  }
}
