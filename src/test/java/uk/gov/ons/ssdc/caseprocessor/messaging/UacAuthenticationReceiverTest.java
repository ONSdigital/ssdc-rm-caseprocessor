package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacAuthenticationDTO;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ExtendWith(MockitoExtension.class)
public class UacAuthenticationReceiverTest {

  private final UUID TEST_CASE_ID = UUID.randomUUID();
  private final String TEST_QID_ID = "1234567890123456";

  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;

  @InjectMocks UacAuthenticationReceiver underTest;

  @Test
  public void testUacAuthentication() {
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("RH");
    managementEvent.setPayload(new PayloadDTO());

    UacAuthenticationDTO uacAuthentication = new UacAuthenticationDTO();
    uacAuthentication.setQid(TEST_QID_ID);
    managementEvent.getPayload().setUacAuthentication(uacAuthentication);

    UacQidLink expectedUacQidLink = new UacQidLink();
    expectedUacQidLink.setUac(TEST_QID_ID);
    Message<byte[]> message = constructMessageWithValidTimeStamp(managementEvent);

    // Given
    when(uacService.findByQid(TEST_QID_ID)).thenReturn(expectedUacQidLink);

    // when
    underTest.receiveMessage(message);

    // then
    InOrder inOrder = inOrder(uacService, eventLogger);

    inOrder.verify(uacService).findByQid(TEST_QID_ID);

    inOrder
        .verify(eventLogger)
        .logUacQidEvent(
            eq(expectedUacQidLink),
            any(OffsetDateTime.class),
            eq("Respondent authenticated"),
            eq(EventType.UAC_AUTHENTICATION),
            eq(managementEvent.getHeader()),
            eq(uacAuthentication),
            any());

    verifyNoMoreInteractions(uacService);
    verifyNoMoreInteractions(eventLogger);
  }
}
