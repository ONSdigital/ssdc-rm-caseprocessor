package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToObject;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.TelephoneCaptureDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@MessageEndpoint
public class TelephoneCaptureReceiver {
  private final UacService uacService;
  private final CaseService caseService;
  private final EventLogger eventLogger;

  private static final String TELEPHONE_CAPTURE_DESCRIPTION = "Telephone capture request received";

  public TelephoneCaptureReceiver(
      UacService uacService, CaseService caseService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "telephoneCaptureInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    ResponseManagementEvent responseManagementEvent =
        convertJsonBytesToObject(message.getPayload(), ResponseManagementEvent.class);

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    TelephoneCaptureDTO telephoneCapturePayload =
        responseManagementEvent.getPayload().getTelephoneCapture();

    Case caze = caseService.getCaseByCaseId(telephoneCapturePayload.getCaseId());

    // Double check the QID does not already exist
    if (uacService.existsByQid(telephoneCapturePayload.getQid())) {

      // If it does exist, check if it is linked to the given case
      UacQidLink existingUacQidLink = uacService.findByQid(telephoneCapturePayload.getQid());
      if (existingUacQidLink.getCaze().getId().equals(telephoneCapturePayload.getCaseId())) {

        // If the QID is already linked to the given case this must be duplicate event, ignore
        return;
      }

      // If not then something has gone wrong, error out
      throw new RuntimeException(
          "Telephone capture QID "
              + telephoneCapturePayload.getQid()
              + " is already linked to a different case");
    }

    createNewUacQidLink(caze, telephoneCapturePayload.getUac(), telephoneCapturePayload.getQid());

    eventLogger.logCaseEvent(
        caze,
        responseManagementEvent.getEvent().getDateTime(),
        TELEPHONE_CAPTURE_DESCRIPTION,
        EventType.TELEPHONE_CAPTURE,
        responseManagementEvent.getEvent(),
        telephoneCapturePayload,
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
    uacService.saveAndEmitUacUpdateEvent(uacQidLink);
  }
}
