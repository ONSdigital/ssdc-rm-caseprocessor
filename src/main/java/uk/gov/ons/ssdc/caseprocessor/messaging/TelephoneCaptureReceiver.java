package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.TelephoneCaptureDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

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
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    TelephoneCaptureDTO telephoneCapturePayload = event.getPayload().getTelephoneCapture();

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

    uacService.createLinkAndEmitNewUacQid(
        caze,
        telephoneCapturePayload.getUac(),
        telephoneCapturePayload.getQid(),
        null,
        event.getHeader().getCorrelationId(),
        event.getHeader().getOriginatingUser());

    eventLogger.logCaseEvent(
        caze,
        event.getHeader().getDateTime(),
        TELEPHONE_CAPTURE_DESCRIPTION,
        EventType.TELEPHONE_CAPTURE,
        event.getHeader(),
        telephoneCapturePayload,
        messageTimestamp);
  }
}
