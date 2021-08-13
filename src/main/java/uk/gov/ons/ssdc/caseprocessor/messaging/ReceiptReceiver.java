package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@MessageEndpoint
public class ReceiptReceiver {
  private final UacService uacService;
  private final CaseService caseService;
  private final EventLogger eventLogger;

  public ReceiptReceiver(UacService uacService, CaseService caseService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  @ServiceActivator(inputChannel = "receiptInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    UacQidLink uacQidLink = uacService.findByQid(event.getPayload().getReceipt().getQid());

    if (uacQidLink.isActive()) {
      uacQidLink.setActive(false);
      uacQidLink = uacService.saveAndEmitUacUpdateEvent(uacQidLink);

      if (uacQidLink.getCaze() != null) {
        Case caze = uacQidLink.getCaze();
        caze.setReceiptReceived(true);
        caseService.saveCaseAndEmitCaseUpdate(caze);
      }
    }

    eventLogger.logUacQidEvent(
        uacQidLink,
        event.getHeader().getDateTime(),
        "Receipt received",
        EventType.RECEIPT,
        event.getHeader(),
        event.getPayload(),
        messageTimestamp);
  }
}
