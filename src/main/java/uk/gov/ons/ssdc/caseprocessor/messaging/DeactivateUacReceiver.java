package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToObject;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@MessageEndpoint
public class DeactivateUacReceiver {
  private final UacService uacService;
  private final EventLogger eventLogger;

  public DeactivateUacReceiver(UacService uacService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "deactivateUacInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    ResponseManagementEvent responseManagementEvent =
        convertJsonBytesToObject(message.getPayload(), ResponseManagementEvent.class);

    UacQidLink uacQidLink =
        uacService.findByQid(responseManagementEvent.getPayload().getDeactivateUac().getQid());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);
    uacQidLink.setActive(false);
    uacQidLink = uacService.saveAndEmitUacUpdatedEvent(uacQidLink);

    eventLogger.logUacQidEvent(
        uacQidLink,
        responseManagementEvent.getEvent().getDateTime(),
        "Deactivate UAC",
        EventType.DEACTIVATE_UAC,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload(),
        messageTimestamp);
  }
}
