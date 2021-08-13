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
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

@MessageEndpoint
public class InvalidCaseReceiver {
  private final CaseService caseService;
  private final EventLogger eventLogger;

  public InvalidCaseReceiver(CaseService caseService, EventLogger eventLogger) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "invalidCaseInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    ResponseManagementEvent responseManagementEvent =
        convertJsonBytesToObject(message.getPayload(), ResponseManagementEvent.class);

    Case caze =
        caseService.getCaseByCaseId(
            responseManagementEvent.getPayload().getInvalidCase().getCaseId());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    caze.setInvalid(true);

    caseService.saveCaseAndEmitCaseUpdate(caze);

    eventLogger.logCaseEvent(
        caze,
        responseManagementEvent.getEvent().getDateTime(),
        "Invalid case",
        EventType.INVALID_CASE,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload(),
        messageTimestamp);
  }
}
