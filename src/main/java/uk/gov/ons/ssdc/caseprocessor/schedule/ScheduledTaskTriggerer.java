package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

@Component
public class ScheduledTaskTriggerer {
  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskTriggerer.class);
  private final ScheduledTaskRepository scheduledTaskRepository;
  private final ScheduledTaskProcessor scheduledTaskProcessor;
  private final String hostName = InetAddress.getLocalHost().getHostName();

  public ScheduledTaskTriggerer(
      ScheduledTaskRepository scheduledTaskRepository,
      ScheduledTaskProcessor scheduledTaskProcessor)
      throws UnknownHostException {
    this.scheduledTaskRepository = scheduledTaskRepository;
    this.scheduledTaskProcessor = scheduledTaskProcessor;
  }

  @Transactional
  public void triggerScheduledTasks() {
    try (Stream<ScheduledTask> tasks = scheduledTaskRepository.findScheduledTasks(100)) {
      List<ScheduledTask> scheduledTasksToDelete = new LinkedList<>();

      log.with("hostName", hostName).info("Scheduled Task processing triggered");

      tasks.forEach(
          scheduledTaskToProcess -> {
            scheduledTaskProcessor.process(scheduledTaskToProcess);
            scheduledTasksToDelete.add(scheduledTaskToProcess);
          });

      scheduledTaskRepository.deleteAllInBatch(scheduledTasksToDelete);
    }
  }
}
