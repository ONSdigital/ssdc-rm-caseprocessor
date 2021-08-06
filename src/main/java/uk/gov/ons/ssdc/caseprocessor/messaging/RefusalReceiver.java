package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

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

@MessageEndpoint
public class RefusalReceiver {

  private final CaseService caseService;
  private final EventLogger eventLogger;

  public RefusalReceiver(CaseService caseService, EventLogger eventLogger) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "refusalInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<ResponseManagementEvent> message) {
    ResponseManagementEvent responseManagementEvent = message.getPayload();

    RefusalDTO refusal = message.getPayload().getPayload().getRefusal();
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
