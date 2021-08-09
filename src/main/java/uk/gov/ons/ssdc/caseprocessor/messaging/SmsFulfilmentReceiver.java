package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.SmsFulfilment;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@MessageEndpoint
public class SmsFulfilmentReceiver {

  private final UacService uacService;
  private final CaseService caseService;
  private final EventLogger eventLogger;

  private static final String SMS_FULFILMENT_DESCRIPTION = "SMS fulfilment request received";

  public SmsFulfilmentReceiver(
      UacService uacService, CaseService caseService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "smsFulfilmentInputChannel")
  public void receiveMessage(Message<ResponseManagementEvent> message) {
    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);
    ResponseManagementEvent responseManagementEvent = message.getPayload();
    SmsFulfilment smsFulfilment = responseManagementEvent.getPayload().getSmsFulfilment();

    UacQidDTO uacQidDTO = responseManagementEvent.getPayload().getUacQidDTO();

    Case caze = caseService.getCaseByCaseId(smsFulfilment.getCaseId());

    // Double check the QID does not already exist
    if (uacService.existsByQid(uacQidDTO.getQid())) {

      // If it does exist, check if it is linked to the given case
      UacQidLink existingUacQidLink = uacService.findByQid(uacQidDTO.getQid());
      if (existingUacQidLink.getCaze().getId() == smsFulfilment.getCaseId()) {

        // If the QID is already linked to the given case this must be duplicate event, ignore
        return;
      }

      // If not then something has gone wrong, error out
      throw new RuntimeException(
          "SMS fulfilment QID " + uacQidDTO.getQid() + " is already linked to a different case");
    }

    createNewUacQidLink(caze, uacQidDTO.getUac(), uacQidDTO.getQid());

    eventLogger.logCaseEvent(
        caze,
        responseManagementEvent.getEvent().getDateTime(),
        SMS_FULFILMENT_DESCRIPTION,
        EventType.SMS_FULFILMENT,
        responseManagementEvent.getEvent(),
        smsFulfilment,
        messageTimestamp);
  }

  private void createNewUacQidLink(Case caze, String uac, String qid) {
    OffsetDateTime now = OffsetDateTime.now();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setUac(uac);
    uacQidLink.setQid(qid);
    uacQidLink.setCaze(caze);
    uacQidLink.setCreatedAt(now);
    uacQidLink.setLastUpdatedAt(now);
    uacService.saveAndEmitUacUpdatedEvent(uacQidLink);
  }
}
