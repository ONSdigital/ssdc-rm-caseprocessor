package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
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

  public ScheduledTaskTriggerer(
      ScheduledTaskRepository scheduledTaskRepository,
      ScheduledTaskProcessor scheduledTaskProcessor) {
    this.scheduledTaskRepository = scheduledTaskRepository;
    this.scheduledTaskProcessor = scheduledTaskProcessor;
  }

  @Transactional
  public void triggerScheduledTasks() {
    try (Stream<ScheduledTask> tasks = scheduledTaskRepository.findScheduledTasks(100)) {
      List<ScheduledTask> scheduledTasksToDelete = new LinkedList<>();

      tasks.forEach(
          scheduledTaskToProcess -> {
            scheduledTaskProcessor.process(scheduledTaskToProcess);
            scheduledTasksToDelete.add(scheduledTaskToProcess);
          });

      scheduledTaskRepository.deleteAllInBatch(scheduledTasksToDelete);
    }
  }
}
