package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ReceiptDTO;

@Service
public class ActionRuleScheduler {
  private static final Logger log = LoggerFactory.getLogger(ActionRuleScheduler.class);
  private final ActionRuleTriggerer actionRuleTriggerer;
  private final ClusterLeaderManager clusterLeaderManager;

  public ActionRuleScheduler(
      ActionRuleTriggerer actionRuleTriggerer, ClusterLeaderManager clusterLeaderManager) {
    this.actionRuleTriggerer = actionRuleTriggerer;
    this.clusterLeaderManager = clusterLeaderManager;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void triggerActionRule() {
//    This is a lazy hack to get instant logging without further effort
    EventDTO event = new EventDTO();
    PayloadDTO payloadDTO = new PayloadDTO();
    ReceiptDTO receiptDTO = new ReceiptDTO();
    receiptDTO.setQid("Testing, testing 123");
    payloadDTO.setReceipt(receiptDTO);
    event.setPayload(payloadDTO);

    log.with(event).error("ERROR!!!");
    log.with(event).warn("WARN!!!");
    log.with(event).info("INFO!!!");
    log.with(event).debug("DEBUGGING!!!");


    if (!clusterLeaderManager.isThisHostClusterLeader()) {
      return; // This host (i.e. pod) is not the leader... don't do any scheduling
    }

    try {
      actionRuleTriggerer.triggerActionRule();
    } catch (Exception e) {
      log.error("Unexpected exception while processing action rule", e);
      throw e;
    }
  }
}
