package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WaveOfContactScheduler {
  private static final Logger log = LoggerFactory.getLogger(WaveOfContactScheduler.class);
  private final WaveOfContactTriggerer waveOfContactTriggerer;
  private final ClusterLeaderManager clusterLeaderManager;

  public WaveOfContactScheduler(
      WaveOfContactTriggerer waveOfContactTriggerer, ClusterLeaderManager clusterLeaderManager) {
    this.waveOfContactTriggerer = waveOfContactTriggerer;
    this.clusterLeaderManager = clusterLeaderManager;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void triggerWaveOfContact() {
    if (!clusterLeaderManager.isThisHostClusterLeader()) {
      return; // This host (i.e. pod) is not the leader... don't do any scheduling
    }

    try {
      waveOfContactTriggerer.triggerWaveOfContact();
    } catch (Exception e) {
      log.error("Unexpected exception while processing wave of contact", e);
      throw e;
    }
  }
}
