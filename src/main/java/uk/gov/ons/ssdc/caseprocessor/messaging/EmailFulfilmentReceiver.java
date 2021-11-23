package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EnrichedEmailFulfilment;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@MessageEndpoint
public class EmailFulfilmentReceiver {

  private final UacService uacService;
  private final CaseService caseService;
  private final EventLogger eventLogger;

  private static final String EMAIL_FULFILMENT_DESCRIPTION = "Email fulfilment request received";

  public EmailFulfilmentReceiver(
      UacService uacService, CaseService caseService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "emailFulfilmentInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());
    EnrichedEmailFulfilment emailFulfilment = event.getPayload().getEnrichedEmailFulfilment();

    Case caze = caseService.getCase(emailFulfilment.getCaseId());

    if (emailFulfilment.getQid() != null) {
      // Check the QID does not already exist
      if (uacService.existsByQid(emailFulfilment.getQid())) {

        // If it does exist, check if it is linked to the given case
        UacQidLink existingUacQidLink = uacService.findByQid(emailFulfilment.getQid());
        if (existingUacQidLink.getCaze().getId().equals(emailFulfilment.getCaseId())) {

          // If the QID is already linked to the given case this must be duplicate event, ignore
          return;
        }

        // If not then something has gone wrong, error out
        throw new RuntimeException(
            "Email fulfilment QID "
                + emailFulfilment.getQid()
                + " is already linked to a different case");
      }
      uacService.createLinkAndEmitNewUacQid(
          caze,
          emailFulfilment.getUac(),
          emailFulfilment.getQid(),
          emailFulfilment.getUacMetadata(),
          event.getHeader().getCorrelationId(),
          event.getHeader().getOriginatingUser());
    }

    eventLogger.logCaseEvent(
        caze, EMAIL_FULFILMENT_DESCRIPTION, EventType.EMAIL_FULFILMENT, event, message);
  }
}
