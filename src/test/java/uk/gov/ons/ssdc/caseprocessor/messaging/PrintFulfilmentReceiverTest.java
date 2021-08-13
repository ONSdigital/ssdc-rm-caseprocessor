package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

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
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintFulfilmentDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentSurveyPrintTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.PrintTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Survey;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

@ExtendWith(MockitoExtension.class)
public class PrintFulfilmentReceiverTest {
  @Mock private CaseService caseService;

  @Mock private EventLogger eventLogger;

  @Mock private FulfilmentToProcessRepository fulfilmentToProcessRepository;

  @InjectMocks private PrintFulfilmentReceiver underTest;

  @Test
  public void testReceiveMessage() {
    // Given
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")).minusHours(1));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setPrintFulfilment(new PrintFulfilmentDTO());
    managementEvent.getPayload().getPrintFulfilment().setCaseId(UUID.randomUUID());
    managementEvent.getPayload().getPrintFulfilment().setPackCode("TEST_FULFILMENT_CODE");
    Message<byte[]> message = constructMessageWithValidTimeStamp(managementEvent);

    PrintTemplate printTemplate = new PrintTemplate();
    printTemplate.setPackCode("TEST_FULFILMENT_CODE");

    FulfilmentSurveyPrintTemplate fulfilmentSurveyPrintTemplate =
        new FulfilmentSurveyPrintTemplate();
    fulfilmentSurveyPrintTemplate.setPrintTemplate(printTemplate);

    Case expectedCase = new Case();
    expectedCase.setCollectionExercise(new CollectionExercise());
    expectedCase.getCollectionExercise().setSurvey(new Survey());
    expectedCase
        .getCollectionExercise()
        .getSurvey()
        .setFulfilmentPrintTemplates(List.of(fulfilmentSurveyPrintTemplate));
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // When
    underTest.receiveMessage(message);

    // Then
    ArgumentCaptor<FulfilmentToProcess> fulfilmentToProcessArgCapt =
        ArgumentCaptor.forClass(FulfilmentToProcess.class);
    verify(fulfilmentToProcessRepository).saveAndFlush(fulfilmentToProcessArgCapt.capture());
    FulfilmentToProcess fulfilmentToProcess = fulfilmentToProcessArgCapt.getValue();
    assertThat(fulfilmentToProcess.getPrintTemplate()).isEqualTo(printTemplate);
    assertThat(fulfilmentToProcess.getCaze()).isEqualTo(expectedCase);

    OffsetDateTime messageDateTime = getMsgTimeStamp(message);

    verify(eventLogger)
        .logCaseEvent(
            eq(expectedCase),
            eq(managementEvent.getHeader().getDateTime()),
            eq("Print fulfilment requested"),
            eq(EventType.PRINT_FULFILMENT),
            eq(managementEvent.getHeader()),
            eq(managementEvent.getPayload()),
            eq(messageDateTime));
  }
}
