package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import java.io.IOException;
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
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@MessageEndpoint
public class DeactivateUacReceiver {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  private final UacService uacService;
  private final EventLogger eventLogger;

  public DeactivateUacReceiver(UacService uacService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "deactivateUacInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    byte[] rawMessageBody = message.getPayload();

    ResponseManagementEvent responseManagementEvent;
    try {
      responseManagementEvent = objectMapper.readValue(rawMessageBody, ResponseManagementEvent.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

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
