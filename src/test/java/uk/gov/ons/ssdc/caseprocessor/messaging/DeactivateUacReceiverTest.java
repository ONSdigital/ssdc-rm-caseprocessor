package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ExtendWith(MockitoExtension.class)
public class DeactivateUacReceiverTest {
  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;

  @InjectMocks DeactivateUacReceiver underTest;

  @Test
  public void testDeactivateUac() {
    // Given
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setCorrelationId(TEST_CORRELATION_ID);
    managementEvent.getHeader().setOriginatingUser(TEST_ORIGINATING_USER);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")).minusHours(1));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("RM");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setDeactivateUac(new DeactivateUacDTO());
    managementEvent.getPayload().getDeactivateUac().setQid("0123456789");

    Message<byte[]> message = constructMessageWithValidTimeStamp(managementEvent);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(true);
    when(uacService.findByQid("0123456789")).thenReturn(uacQidLink);
    when(uacService.saveAndEmitUacUpdateEvent(any(UacQidLink.class), any(UUID.class), anyString()))
        .thenReturn(uacQidLink);

    // When
    underTest.receiveMessage(message);

    // Then
    ArgumentCaptor<UacQidLink> uacQidLinkArgumentCaptor = ArgumentCaptor.forClass(UacQidLink.class);

    verify(uacService)
        .saveAndEmitUacUpdateEvent(
            uacQidLinkArgumentCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    UacQidLink actualUacQidLink = uacQidLinkArgumentCaptor.getValue();
    assertThat(actualUacQidLink.isActive()).isFalse();

    OffsetDateTime messageDateTime = getMsgTimeStamp(message);

    verify(eventLogger)
        .logUacQidEvent(
            eq(uacQidLink),
            eq(managementEvent.getHeader().getDateTime()),
            eq("Deactivate UAC"),
            eq(EventType.DEACTIVATE_UAC),
            eq(managementEvent.getHeader()),
            eq(managementEvent.getPayload()),
            eq(messageDateTime));
  }
}
