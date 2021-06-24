package uk.gov.ons.ssdc.caseprocessor.messaging;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

import java.time.OffsetDateTime;

import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

@MessageEndpoint
public class FulfilmentReceiver {

  private final CaseService caseService;
  private final EventLogger eventLogger;
  private final FulfilmentToProcessRepository fulfilmentToProcessRepository;

  public FulfilmentReceiver(
      CaseService caseService,
      EventLogger eventLogger,
      FulfilmentToProcessRepository fulfilmentToProcessRepository) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
    this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "fulfilmentInputChannel")
  public void receiveMessage(Message<ResponseManagementEvent> message) {
    ResponseManagementEvent responseManagementEvent = message.getPayload();
    Case caze =
        caseService.getCaseByCaseId(
            responseManagementEvent.getPayload().getFulfilment().getCaseId());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();

    fulfilmentToProcess.setFulfilmentCode(
        responseManagementEvent.getPayload().getFulfilment().getFulfilmentCode());
    fulfilmentToProcess.setCaze(caze);

    fulfilmentToProcessRepository.saveAndFlush(fulfilmentToProcess);

    eventLogger.logCaseEvent(
        caze,
        responseManagementEvent.getEvent().getDateTime(),
        "Fulfilment requested",
        EventType.FULFILMENT,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload(),
        messageTimestamp);
  }
}
