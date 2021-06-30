package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO.FULFILMENT;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.FulfilmentDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

@RunWith(MockitoJUnitRunner.class)
public class FulfilmentReceiverTest {
  @Mock private CaseService caseService;

  @Mock private EventLogger eventLogger;

  @Mock private FulfilmentToProcessRepository fulfilmentToProcessRepository;

  @InjectMocks private FulfilmentReceiver underTest;

  @Test
  public void testReceiveMessage() {
    // Given
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setDateTime(OffsetDateTime.now().minusHours(1));
    managementEvent.getEvent().setType(FULFILMENT);
    managementEvent.getEvent().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setFulfilment(new FulfilmentDTO());
    managementEvent.getPayload().getFulfilment().setCaseId(UUID.randomUUID());
    managementEvent.getPayload().getFulfilment().setFulfilmentCode("TEST_FULFILMENT_CODE");
    Message<ResponseManagementEvent> message = constructMessageWithValidTimeStamp(managementEvent);
    Case expectedCase = new Case();
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // When
    underTest.receiveMessage(message);

    // Then
    ArgumentCaptor<FulfilmentToProcess> fulfilmentToProcessArgCapt =
        ArgumentCaptor.forClass(FulfilmentToProcess.class);
    verify(fulfilmentToProcessRepository).saveAndFlush(fulfilmentToProcessArgCapt.capture());
    FulfilmentToProcess fulfilmentToProcess = fulfilmentToProcessArgCapt.getValue();
    assertThat(fulfilmentToProcess.getFulfilmentCode()).isEqualTo("TEST_FULFILMENT_CODE");
    assertThat(fulfilmentToProcess.getCaze()).isEqualTo(expectedCase);

    OffsetDateTime messageDateTime = getMsgTimeStamp(message);

    verify(eventLogger)
        .logCaseEvent(
            eq(expectedCase),
            eq(managementEvent.getEvent().getDateTime()),
            eq("Fulfilment requested"),
            eq(EventType.FULFILMENT),
            eq(managementEvent.getEvent()),
            eq(managementEvent.getPayload()),
            eq(messageDateTime));
  }
}
