package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_UAC_METADATA;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ExportFileFulfilmentDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.common.model.entity.Survey;

@ExtendWith(MockitoExtension.class)
class PrintFulfilmentReceiverTest {
  @Mock private CaseService caseService;

  @Mock private EventLogger eventLogger;

  @Mock private FulfilmentToProcessRepository fulfilmentToProcessRepository;

  @InjectMocks private PrintFulfilmentReceiver underTest;

  private static final String PACK_CODE = "PACK_CODE";

  @Test
  void testReceiveMessage() {
    // Given
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")).minusHours(1));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setExportFileFulfilment(new ExportFileFulfilmentDTO());
    managementEvent.getPayload().getExportFileFulfilment().setCaseId(UUID.randomUUID());
    managementEvent.getPayload().getExportFileFulfilment().setPackCode(PACK_CODE);
    managementEvent.getPayload().getExportFileFulfilment().setUacMetadata(TEST_UAC_METADATA);
    Message<byte[]> message = constructMessage(managementEvent);

    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setPackCode(PACK_CODE);

    FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate =
        new FulfilmentSurveyExportFileTemplate();
    fulfilmentSurveyExportFileTemplate.setExportFileTemplate(exportFileTemplate);

    Case expectedCase = new Case();
    expectedCase.setCollectionExercise(new CollectionExercise());
    expectedCase.getCollectionExercise().setSurvey(new Survey());
    expectedCase
        .getCollectionExercise()
        .getSurvey()
        .setFulfilmentExportFileTemplates(List.of(fulfilmentSurveyExportFileTemplate));
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // When
    underTest.receiveMessage(message);

    // Then
    ArgumentCaptor<FulfilmentToProcess> fulfilmentToProcessArgCapt =
        ArgumentCaptor.forClass(FulfilmentToProcess.class);
    verify(fulfilmentToProcessRepository).saveAndFlush(fulfilmentToProcessArgCapt.capture());
    FulfilmentToProcess fulfilmentToProcess = fulfilmentToProcessArgCapt.getValue();
    assertThat(fulfilmentToProcess.getExportFileTemplate()).isEqualTo(exportFileTemplate);
    assertThat(fulfilmentToProcess.getCaze()).isEqualTo(expectedCase);
    assertThat(fulfilmentToProcess.getUacMetadata()).isEqualTo(TEST_UAC_METADATA);

    verify(eventLogger)
        .logCaseEvent(
            eq(expectedCase),
            eq("Print fulfilment requested"),
            eq(EventType.PRINT_FULFILMENT),
            eq(managementEvent),
            eq(message));
  }
}
