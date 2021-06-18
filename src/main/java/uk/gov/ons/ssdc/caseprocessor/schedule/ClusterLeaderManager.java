package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ClusterLeader;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ClusterLeaderRepository;

@Component
public class ClusterLeaderManager {
  private static final Logger log = LoggerFactory.getLogger(ClusterLeaderManager.class);
  private static final UUID LEADER_ID = UUID.fromString("e469807b-f2e2-47bd-acf6-74f8943ff3db");

  private final ClusterLeaderRepository clusterLeaderRepository;

  @Value("${scheduler.leaderDeathTimeout}")
  private int leaderDeathTimeout;

  private String hostName = InetAddress.getLocalHost().getHostName();

  public ClusterLeaderManager(ClusterLeaderRepository clusterLeaderRepository)
      throws UnknownHostException {
    this.clusterLeaderRepository = clusterLeaderRepository;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public boolean isThisHostClusterLeader() {
    if (!clusterLeaderRepository.existsById(LEADER_ID)) {
      ClusterLeader clusterLeader = new ClusterLeader();
      clusterLeader.setId(LEADER_ID);
      clusterLeader.setHostName(hostName);
      clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now());
      clusterLeaderRepository.saveAndFlush(clusterLeader);

      log.with("hostName", hostName)
          .debug("No leader existed in DB, so this host is attempting to become leader");
    }

    Optional<ClusterLeader> lockedClusterLeaderOpt =
        clusterLeaderRepository.getClusterLeaderAndLockById(LEADER_ID);

    if (!lockedClusterLeaderOpt.isPresent()) {
      log.with("hostName", hostName)
          .debug("Could not get leader row, presumably because of lock contention");
      return false;
    }

    ClusterLeader clusterLeader = lockedClusterLeaderOpt.get();

    if (clusterLeader.getHostName().equals(hostName)) {
      log.with("hostName", hostName).debug("This host is leader");
      return true;
    } else if (clusterLeader
        .getHostLastSeenAliveAt()
        .isBefore(OffsetDateTime.now().minusSeconds(leaderDeathTimeout))) {
      String oldHostName = clusterLeader.getHostName();
      clusterLeader.setHostName(hostName);
      clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now());
      clusterLeaderRepository.saveAndFlush(clusterLeader);

      log.with("oldHostName", oldHostName)
          .with("hostName", hostName)
          .debug("Leader has transferred from dead host to this host");
      return true;
    }

    log.with("hostName", hostName).debug("This host is not the leader");
    return false;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public void leaderKeepAlive() {
    Optional<ClusterLeader> lockedClusterLeaderOpt =
        clusterLeaderRepository.getClusterLeaderAndLockById(LEADER_ID);

    if (!lockedClusterLeaderOpt.isPresent()) {
      return;
    }

    ClusterLeader clusterLeader = lockedClusterLeaderOpt.get();

    if (clusterLeader.getHostName().equals(hostName)) {
      clusterLeader.setHostLastSeenAliveAt(OffsetDateTime.now());
      clusterLeaderRepository.saveAndFlush(clusterLeader);

      log.with("hostName", hostName).debug("Leader keepalive updated. This host is leader");
    }
  }
}
