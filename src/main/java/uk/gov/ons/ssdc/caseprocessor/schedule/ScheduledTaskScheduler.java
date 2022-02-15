package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskScheduler {
  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskScheduler.class);
  private final ScheduledTaskTriggerer scheduledTaskTriggerer;
  private final ClusterLeaderManager clusterLeaderManager;

  public ScheduledTaskScheduler(
      ScheduledTaskTriggerer scheduledTaskTriggerer, ClusterLeaderManager clusterLeaderManager) {
    this.scheduledTaskTriggerer = scheduledTaskTriggerer;
    this.clusterLeaderManager = clusterLeaderManager;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void triggerScheduledTasks() {
    //    Cluster leader or cluster ....
    //    if (!clusterLeaderManager.isThisHostClusterLeader()) {
    //      return; // This host (i.e. pod) is not the leader... don't do any scheduling
    //    }

    try {
      scheduledTaskTriggerer.triggerScheduledTasks();
    } catch (Exception e) {
      log.error("Unexpected exception while processing scheduledTask", e);
      throw e;
    }
  }
}
