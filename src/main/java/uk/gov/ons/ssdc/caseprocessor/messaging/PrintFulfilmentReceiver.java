package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintFulfilmentDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.FulfilmentService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;

@MessageEndpoint
public class PrintFulfilmentReceiver {
  private final CaseService caseService;
  private final EventLogger eventLogger;
  private final FulfilmentService fulfilmentService;

  public PrintFulfilmentReceiver(
      CaseService caseService, EventLogger eventLogger, FulfilmentService fulfilmentService) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
    this.fulfilmentService = fulfilmentService;
  }

  @Transactional
  @ServiceActivator(inputChannel = "printFulfilmentInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    Case caze = caseService.getCase(event.getPayload().getPrintFulfilment().getCaseId());
    PrintFulfilmentDTO fulfilment = event.getPayload().getPrintFulfilment();

    fulfilmentService.processPrintFulfilment(
        caze,
        fulfilment.getPackCode(),
        event.getHeader().getCorrelationId(),
        event.getHeader().getOriginatingUser(),
        fulfilment.getUacMetadata(),
        null);

    eventLogger.logCaseEvent(
        caze, "Print fulfilment requested", EventType.PRINT_FULFILMENT, event, message);
  }
}
