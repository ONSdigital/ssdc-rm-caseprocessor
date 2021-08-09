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
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.RefusalType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@MessageEndpoint
public class RefusalReceiver {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  private final CaseService caseService;
  private final EventLogger eventLogger;

  public RefusalReceiver(CaseService caseService, EventLogger eventLogger) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "refusalInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    byte[] rawMessageBody = message.getPayload();

    ResponseManagementEvent responseManagementEvent;
    try {
      responseManagementEvent = objectMapper.readValue(rawMessageBody, ResponseManagementEvent.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    RefusalDTO refusal = responseManagementEvent.getPayload().getRefusal();
    Case refusedCase = caseService.getCaseByCaseId(refusal.getCollectionCase().getCaseId());
    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);
    refusedCase.setRefusalReceived(RefusalType.valueOf(refusal.getType().name()));

    caseService.saveCaseAndEmitCaseUpdatedEvent(refusedCase);

    eventLogger.logCaseEvent(
        refusedCase,
        responseManagementEvent.getEvent().getDateTime(),
        "Refusal Received",
        EventType.REFUSAL_RECEIVED,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload(),
        messageTimestamp);
  }
}
