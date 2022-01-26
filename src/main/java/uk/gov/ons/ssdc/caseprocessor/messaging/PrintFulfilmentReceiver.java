package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.FuflilmentService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.common.model.entity.Survey;

@MessageEndpoint
public class PrintFulfilmentReceiver {
  private final CaseService caseService;
  private final EventLogger eventLogger;
  private final FuflilmentService fuflilmentService;


  public PrintFulfilmentReceiver(
      CaseService caseService,
      EventLogger eventLogger,
      FuflilmentService fuflilmentService) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
    this.fuflilmentService = fuflilmentService;
  }

  @Transactional
  @ServiceActivator(inputChannel = "printFulfilmentInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO eventDto = convertJsonBytesToEvent(message.getPayload());
    Case caze = caseService.getCase(eventDto.getPayload().getPrintFulfilment().getCaseId());
    fuflilmentService.processPrintFulfilment(caze, eventDto);

    eventLogger.logCaseEvent(
        caze, "Print fulfilment requested", EventType.PRINT_FULFILMENT, eventDto, message);
  }
}
