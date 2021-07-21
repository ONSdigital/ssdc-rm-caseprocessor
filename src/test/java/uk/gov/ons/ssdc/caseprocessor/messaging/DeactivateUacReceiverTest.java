package uk.gov.ons.ssdc.caseprocessor.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DeactivateUacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO.DEACTIVATE_UAC;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

@ExtendWith(MockitoExtension.class)
public class DeactivateUacReceiverTest {

  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;

  @InjectMocks DeactivateUacReceiver underTest;

  @Test
  public void testInvalidAddress() {
    // Given
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setDateTime(OffsetDateTime.now().minusHours(1));
    managementEvent.getEvent().setType(DEACTIVATE_UAC);
    managementEvent.getEvent().setChannel("RM");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setDeactivateUac(new DeactivateUacDTO());
    managementEvent.getPayload().getDeactivateUac().setQid("0123456789");

    Message<ResponseManagementEvent> message = constructMessageWithValidTimeStamp(managementEvent);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(true);
    when(uacService.findByQid("0123456789")).thenReturn(uacQidLink);
    when(uacService.saveAndEmitUacUpdatedEvent(any(UacQidLink.class))).thenReturn(uacQidLink);

    // When
    underTest.receiveMessage(message);

    // Then
    ArgumentCaptor<UacQidLink> uacQidLinkArgumentCaptor = ArgumentCaptor.forClass(UacQidLink.class);

    verify(uacService).saveAndEmitUacUpdatedEvent(uacQidLinkArgumentCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkArgumentCaptor.getValue();
    assertThat(actualUacQidLink.isActive()).isFalse();

    OffsetDateTime messageDateTime = getMsgTimeStamp(message);

    verify(eventLogger)
        .logUacQidEvent(
            eq(uacQidLink),
            eq(managementEvent.getEvent().getDateTime()),
            eq("Deactivate UAC"),
            eq(EventType.DEACTIVATE_UAC),
            eq(managementEvent.getEvent()),
            eq(managementEvent.getPayload()),
            eq(messageDateTime));
  }
}
