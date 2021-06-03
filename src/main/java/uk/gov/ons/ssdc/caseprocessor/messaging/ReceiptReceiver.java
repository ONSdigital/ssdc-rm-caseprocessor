package uk.gov.ons.ssdc.caseprocessor.messaging;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@MessageEndpoint
public class ReceiptReceiver {

  private final UacService uacService;
  private final CaseService caseService;

  public ReceiptReceiver(UacService uacService, CaseService caseService) {
    this.uacService = uacService;
    this.caseService = caseService;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  @ServiceActivator(inputChannel = "receiptInputChannel")
  public void receiveMessage(ResponseManagementEvent responseManagementEvent) {

    UacQidLink uacQidLink =
        uacService.findByQid(
            responseManagementEvent.getPayload().getResponse().getQuestionnaireId());

    // if uacQidLink is null it'll blow up, do we code defensively instead?

    if (uacQidLink.isActive()) {
      uacQidLink.setActive(false);
      uacService.saveAndEmitUacUpdatedEvent(uacQidLink);

      if (uacQidLink.getCaze() != null) {
        Case caze = uacQidLink.getCaze();
        caze.setReceiptReceived(true);
        caseService.saveCaseAndEmitCaseUpdatedEvent(caze);
      }
    }
  }
}
