package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@MessageEndpoint
public class UacAuthenticationReceiver {
  private final UacService uacService;
  private final EventLogger eventLogger;

  public UacAuthenticationReceiver(UacService uacService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "uacAuthenticationInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    UacQidLink uacQidLink =
        uacService.findByQid(event.getPayload().getUacAuthentication().getQid());

    eventLogger.logUacQidEvent(
        uacQidLink,
        event.getHeader().getDateTime(),
        "Respondent authenticated",
        EventType.UAC_AUTHENTICATION,
        event.getHeader(),
        event.getPayload().getUacAuthentication(),
        messageTimestamp);
  }
}
