package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ScheduledTaskTriggerer {
  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskTriggerer.class);
  private final ScheduledTaskRepository scheduledTaskRepository;
  private final ActionRuleProcessor actionRuleProcessor;

  private String hostName = InetAddress.getLocalHost().getHostName();

  public ScheduledTaskTriggerer(
          ScheduledTaskRepository scheduledTaskRepository, ActionRuleProcessor actionRuleProcessor)
      throws UnknownHostException {
    this.scheduledTaskRepository = scheduledTaskRepository;

    this.actionRuleProcessor = actionRuleProcessor;
  }

  @Transactional
  public void triggerScheduledTasks() {
    List<ScheduledTask> scheduledTasks =
        scheduledTaskRepository.findByrmToActionDateTimeBeforeAndScheduledTaskStateEquals(
            OffsetDateTime.now(), ScheduledTaskState.NOT_STARTED);

    for( ScheduledTask scheduledTask : scheduledTasks ) {
        ScheduledTaskProcessor.process(scheduledTask);
    }
  }
}
