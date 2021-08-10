package uk.gov.ons.ssdc.caseprocessor.messaging;

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
public class InvalidAddressReceiver {

  private final CaseService caseService;
  private final EventLogger eventLogger;

  public InvalidAddressReceiver(CaseService caseService, EventLogger eventLogger) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "invalidAddressInputChannel")
  public void receiveMessage(Message<ResponseManagementEvent> message) {
    ResponseManagementEvent responseManagementEvent = message.getPayload();
    Case caze =
        caseService.getCaseByCaseId(
            responseManagementEvent.getPayload().getInvalidAddress().getCaseId());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    caze.setAddressInvalid(true);

    caseService.saveCaseAndEmitCaseUpdatedEvent(caze);

    eventLogger.logCaseEvent(
        caze,
        responseManagementEvent.getEvent().getDateTime(),
        "Invalid address",
        EventType.ADDRESS_NOT_VALID,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload(),
        messageTimestamp,
        responseManagementEvent.getEvent().getCreatedBy());
  }
}
