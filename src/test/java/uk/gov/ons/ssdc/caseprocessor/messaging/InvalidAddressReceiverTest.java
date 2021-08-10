package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO.ADDRESS_NOT_VALID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.InvalidAddress;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

@ExtendWith(MockitoExtension.class)
public class InvalidAddressReceiverTest {

  @Mock private CaseService caseService;
  @Mock private EventLogger eventLogger;

  @InjectMocks InvalidAddressReceiver underTest;

  @Test
  public void testInvalidAddress() {
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setDateTime(OffsetDateTime.now().minusHours(1));
    managementEvent.getEvent().setType(ADDRESS_NOT_VALID);
    managementEvent.getEvent().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setInvalidAddress(new InvalidAddress());
    managementEvent.getPayload().getInvalidAddress().setCaseId(UUID.randomUUID());
    Message<ResponseManagementEvent> message = constructMessageWithValidTimeStamp(managementEvent);

    // Given
    Case expectedCase = new Case();
    expectedCase.setAddressInvalid(false);
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // when
    underTest.receiveMessage(message);

    // then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);

    verify(caseService).saveCaseAndEmitCaseUpdatedEvent(caseArgumentCaptor.capture());
    Case actualCase = caseArgumentCaptor.getValue();
    assertThat(actualCase.isAddressInvalid()).isTrue();

    OffsetDateTime messageDateTime = getMsgTimeStamp(message);

    verify(eventLogger)
        .logCaseEvent(
            eq(expectedCase),
            eq(managementEvent.getEvent().getDateTime()),
            eq("Invalid address"),
            eq(EventType.ADDRESS_NOT_VALID),
            eq(managementEvent.getEvent()),
            eq(managementEvent.getPayload()),
            eq(messageDateTime),
            any());
  }
}
