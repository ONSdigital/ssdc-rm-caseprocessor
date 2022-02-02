package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;

@Component
public class ScheduledTaskTriggerer {
  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskTriggerer.class);
  private final ScheduledTaskRepository scheduledTaskRepository;
  private final ScheduledTaskProcessor scheduledTaskProcessor;

  private String hostName = InetAddress.getLocalHost().getHostName();

  public ScheduledTaskTriggerer(
      ScheduledTaskRepository scheduledTaskRepository,
      ScheduledTaskProcessor scheduledTaskProcessor)
      throws UnknownHostException {
    this.scheduledTaskRepository = scheduledTaskRepository;
    this.scheduledTaskProcessor = scheduledTaskProcessor;
  }

  @Transactional
  public void triggerScheduledTasks() {
    //    List<ScheduledTask> scheduledTasks =
    //        scheduledTaskRepository.findByrmToActionDateTimeBeforeAndScheduledTaskStateEquals(
    //            OffsetDateTime.now(), ScheduledTaskState.NOT_STARTED);

    List<ScheduledTask> scheduledTasks = scheduledTaskRepository.findAll();

    for (ScheduledTask scheduledTask : scheduledTasks) {

      if(scheduledTask.getRmToActionDate().isAfter(OffsetDateTime.now())) {
        continue;
      }

      if (scheduledTask.getActionState() != ScheduledTaskState.NOT_STARTED) {
        continue;
      }

      scheduledTaskProcessor.process(scheduledTask);
    }
  }
}
