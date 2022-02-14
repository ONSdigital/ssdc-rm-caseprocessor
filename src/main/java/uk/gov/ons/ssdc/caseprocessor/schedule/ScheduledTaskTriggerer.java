package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskStatus;

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

    //    Needs fancy pants JPA and UPDATE FOR SKIP LOCK?...., but for now this will do
    //    With our cluster leader stuff do we need UPDATE FOR SKIP LOCK?
    //    List<ScheduledTask> scheduledTasks =
    //        scheduledTaskRepository.findByrmToActionDateTimeBeforeAndScheduledTaskStateEquals(
    //            OffsetDateTime.now(), ScheduledTaskState.NOT_STARTED);

    List<ScheduledTask> scheduledTasks = scheduledTaskRepository.findAll();

    for (ScheduledTask scheduledTask : scheduledTasks) {

      if (scheduledTask.getRmToActionDate().isAfter(OffsetDateTime.now())) {
        continue;
      }

      if (scheduledTask.getScheduledTaskStatus() != ScheduledTaskStatus.NOT_STARTED) {
        continue;
      }

      scheduledTaskProcessor.process(scheduledTask);

      // finally remove this task from the DB.
      scheduledTaskRepository.deleteById(scheduledTask.getId());
    }
  }
}
