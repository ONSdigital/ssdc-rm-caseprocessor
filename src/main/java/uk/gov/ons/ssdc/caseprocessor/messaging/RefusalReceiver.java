package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.RefusalType;

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
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    RefusalDTO refusal = event.getPayload().getRefusal();
    Case refusedCase = caseService.getCase(refusal.getCaseId());
    refusedCase.setRefusalReceived(RefusalType.valueOf(refusal.getType().name()));

    if (refusal.isEraseData()) {
      refusedCase.setSampleSensitive(null);
      refusedCase.setInvalid(true);
      eventLogger.logCaseEvent(
          refusedCase, "Data erasure request received", EventType.ERASE_DATA, event, message);
    }
    caseService.saveCaseAndEmitCaseUpdate(
        refusedCase, event.getHeader().getCorrelationId(), event.getHeader().getOriginatingUser());

    eventLogger.logCaseEvent(refusedCase, "Refusal Received", EventType.REFUSAL, event, message);
  }
}
