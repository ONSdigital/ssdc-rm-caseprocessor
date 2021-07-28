package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ClusterLeader;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ClusterLeaderRepository;

@Component
public class ClusterLeaderManagerStartup {
  private static final Logger log = LoggerFactory.getLogger(ClusterLeaderManagerStartup.class);
  private final ClusterLeaderRepository clusterLeaderRepository;

  private String hostName = InetAddress.getLocalHost().getHostName();

  public ClusterLeaderManagerStartup(ClusterLeaderRepository clusterLeaderRepository)
      throws UnknownHostException {
    this.clusterLeaderRepository = clusterLeaderRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void doStartupChecksAndAttemptToElectLeaderIfRequired(UUID leaderId) {
    if (clusterLeaderRepository.existsById(leaderId)) {
      return; // We are not staring up for the first time... nothing to do here
    }

    // We ARE starting up for the first time!
    // Record that this host is the cluster leader in the database
    ClusterLeader clusterLeader = new ClusterLeader();
    clusterLeader.setId(leaderId);
    clusterLeader.setHostName(hostName);
    clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now());
    clusterLeaderRepository.saveAndFlush(clusterLeader);

    log.with("hostName", hostName)
        .debug("No leader existed in DB, so this host is attempting to become leader");
  }
}
