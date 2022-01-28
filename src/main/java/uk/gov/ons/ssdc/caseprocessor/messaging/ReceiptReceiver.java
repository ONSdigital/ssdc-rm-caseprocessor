package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.ScheduledTaskService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@MessageEndpoint
public class ReceiptReceiver {
  private final UacService uacService;
  private final EventLogger eventLogger;
  private final ScheduledTaskService scheduledTaskService;

  public ReceiptReceiver(UacService uacService, EventLogger eventLogger, ScheduledTaskService scheduledTaskService) {
    this.uacService = uacService;
    this.eventLogger = eventLogger;
    this.scheduledTaskService = scheduledTaskService;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  @ServiceActivator(inputChannel = "receiptInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    UacQidLink uacQidLink = uacService.findByQid(event.getPayload().getReceipt().getQid());

    if (!uacQidLink.isReceiptReceived()) {
      uacQidLink.setActive(false);
      uacQidLink.setReceiptReceived(true);

      uacQidLink =
          uacService.saveAndEmitUacUpdateEvent(
              uacQidLink,
              event.getHeader().getCorrelationId(),
              event.getHeader().getOriginatingUser());
    }

    Event loggedEvent = eventLogger.logUacQidEvent(uacQidLink, "Receipt received", EventType.RECEIPT, event, message);

    // Problem with this, what happens if the ScheduledTask is removed - this goes boom.
    // Although we may want to remove/deativate any UAC that was attached to a dead ScheduledTask?
    // Also death
    if(uacQidLink.getScheduledTaskId() != null) {
        scheduledTaskService.receiptScheduledTask(uacQidLink.getScheduledTaskId(), loggedEvent);
    }
  }
}
