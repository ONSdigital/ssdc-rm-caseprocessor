package uk.gov.ons.ssdc.caseprocessor.messaging;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.PRINT_FULFILMENT_TOPIC;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintFulfilmentDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
class PrintFulfilmentReceiverIT {

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;
  @Autowired private FulfilmentToProcessRepository fulfilmentToProcessRepository;

  @BeforeEach
  public void setUp() {
    deleteDataHelper.deleteAllData();
  }

  @AfterEach
  public void tearDown() {
    deleteDataHelper.deleteAllData();
  }

  @Test
  void testPrintFulfilment() throws InterruptedException {

    // Given
    Case caze = junkDataHelper.setupJunkCase();
    ExportFileTemplate exportFileTemplate =
        junkDataHelper.setUpJunkExportFileTemplate(new String[] {"__request__.name"});
    junkDataHelper.linkExportFileTemplateToSurveyFulfilment(
        exportFileTemplate, caze.getCollectionExercise().getSurvey());

    EventDTO printFulfilmentEvent = new EventDTO();
    printFulfilmentEvent.setHeader(new EventHeaderDTO());
    junkDataHelper.junkify(printFulfilmentEvent.getHeader());
    printFulfilmentEvent.getHeader().setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    printFulfilmentEvent.getHeader().setTopic(PRINT_FULFILMENT_TOPIC);
    printFulfilmentEvent.setPayload(new PayloadDTO());
    printFulfilmentEvent.getPayload().setPrintFulfilment(new PrintFulfilmentDTO());
    printFulfilmentEvent.getPayload().getPrintFulfilment().setCaseId(caze.getId());
    printFulfilmentEvent
        .getPayload()
        .getPrintFulfilment()
        .setPackCode(exportFileTemplate.getPackCode());

    Map<String, String> personalisation = Map.of("name", "Joe Bloggs");
    printFulfilmentEvent.getPayload().getPrintFulfilment().setPersonalisation(personalisation);
    Map<String, String> uacMetadata = Map.of("foo", "bar");
    printFulfilmentEvent.getPayload().getPrintFulfilment().setUacMetadata(uacMetadata);

    // When
    pubsubHelper.sendMessageToSharedProject(PRINT_FULFILMENT_TOPIC, printFulfilmentEvent);

    // Then
    List<FulfilmentToProcess> fulfilmentsToProcess = getFulfilmentsToProcess();
    assertThat(fulfilmentsToProcess).hasSize(1);
    FulfilmentToProcess fulfilmentToProcess = fulfilmentsToProcess.get(0);

    assertThat(fulfilmentToProcess.getCorrelationId())
        .isEqualTo(printFulfilmentEvent.getHeader().getCorrelationId());
    assertThat(fulfilmentToProcess.getCaze().getId()).isEqualTo(caze.getId());
    assertThat(fulfilmentToProcess.getExportFileTemplate()).isEqualTo(exportFileTemplate);
    assertThat(fulfilmentToProcess.getPersonalisation()).isEqualTo(personalisation);
    assertThat(fulfilmentToProcess.getUacMetadata()).isEqualTo(uacMetadata);
  }

  private List<FulfilmentToProcess> getFulfilmentsToProcess() throws InterruptedException {
    List<FulfilmentToProcess> fulfilmentsToProcess;
    for (int i = 0; i < 10; i++) {
      fulfilmentsToProcess = fulfilmentToProcessRepository.findAll();
      if (fulfilmentsToProcess.size() > 0) {
        return fulfilmentsToProcess;
      } else {
        sleep(1000);
      }
    }
    return List.of();
  }
}
