package uk.gov.ons.ssdc.caseprocessor.messaging;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.RefusalType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

@MessageEndpoint
public class RefusalReceiver {

  private final CaseService caseService;

  public RefusalReceiver(CaseService caseService) {
    this.caseService = caseService;
  }

  @Transactional
  @ServiceActivator(inputChannel = "refusalInputChannel")
  public void receiveMessage(ResponseManagementEvent responseManagementEvent) {
    RefusalDTO refusal = responseManagementEvent.getPayload().getRefusal();
    Case refusedCase = caseService.getCaseByCaseId(refusal.getCaseId());

    refusedCase.setRefusalReceived(RefusalType.valueOf(refusal.getType().name()));

    caseService.saveCaseAndEmitCaseUpdatedEvent(refusedCase);
  }
}
