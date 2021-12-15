package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.common.model.entity.Case;

import java.util.UUID;

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

    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    caze.setCaseRef(2324L);

    log.with("Case: ", caze).info("Testing, testing 123");

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
