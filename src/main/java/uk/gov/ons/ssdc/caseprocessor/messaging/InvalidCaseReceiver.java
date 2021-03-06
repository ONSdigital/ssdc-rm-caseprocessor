package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;

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
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    Case caze = caseService.getCase(event.getPayload().getInvalidCase().getCaseId());

    caze.setInvalid(true);

    caseService.saveCaseAndEmitCaseUpdate(
        caze, event.getHeader().getCorrelationId(), event.getHeader().getOriginatingUser());

    eventLogger.logCaseEvent(caze, "Invalid case", EventType.INVALID_CASE, event, message);
  }
}
