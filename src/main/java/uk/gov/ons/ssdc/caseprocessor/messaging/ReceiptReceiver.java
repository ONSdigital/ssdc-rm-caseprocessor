package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@MessageEndpoint
public class ReceiptReceiver {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

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
    byte[] rawMessageBody = message.getPayload();

    ResponseManagementEvent responseManagementEvent;
    try {
      responseManagementEvent = objectMapper.readValue(rawMessageBody, ResponseManagementEvent.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    UacQidLink uacQidLink =
        uacService.findByQid(
            responseManagementEvent.getPayload().getResponse().getQuestionnaireId());

    if (uacQidLink.isActive()) {
      uacQidLink.setActive(false);
      uacQidLink = uacService.saveAndEmitUacUpdatedEvent(uacQidLink);

      if (uacQidLink.getCaze() != null) {
        Case caze = uacQidLink.getCaze();
        caze.setReceiptReceived(true);
        caseService.saveCaseAndEmitCaseUpdatedEvent(caze);
      }
    }

    eventLogger.logUacQidEvent(
        uacQidLink,
        responseManagementEvent.getEvent().getDateTime(),
        "QID Receipted",
        EventType.RESPONSE_RECEIVED,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload(),
        messageTimestamp);
  }
}
