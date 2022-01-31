package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.service.FulfilmentService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ResponsePeriod;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;

@Component
public class ScheduledTaskProcessor {
  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskProcessor.class);

  private final ScheduledTaskRepository scheduledTaskRepository;
  private final FulfilmentService fulfilmentService;

  public ScheduledTaskProcessor(
      ScheduledTaskRepository scheduledTaskRepository, FulfilmentService fulfilmentService) {
    this.scheduledTaskRepository = scheduledTaskRepository;
    this.fulfilmentService = fulfilmentService;
  }

  public void process(ScheduledTask scheduledTask) {
    Map<String, String> scheduledTaskDetails = scheduledTask.getScheduledTaskDetails();
    ScheduledTaskType scheduledTaskType =
        ScheduledTaskType.valueOf(scheduledTaskDetails.get("type"));
    // In future we could unpack these into a variety of objects?
    // Some sort of switch etc.

    // What wasn't here when I did this was caze.preferences.
    // We'd look at that for the Type e.g. Reminder, PCR, EQ and find the contact preference.
    // So this would look very different

    switch (scheduledTaskType) {
      case ACTION_WITH_PACKCODE:
        processActionWithPackCode(scheduledTask, scheduledTaskDetails);
        break;

      case COMPLETION:
        processsCompletionCheck(scheduledTask, scheduledTaskDetails);
        break;

      default:
        {
          throw new RuntimeException("Scheduled Task Type unknown: " + scheduledTaskType);
        }
    }
  }

  private void processsCompletionCheck(
      ScheduledTask scheduledTask, Map<String, String> scheduledTaskDetails) {
    if (scheduledTaskDetails.get("type").equals("COMPLETION")) {
      if (areAllReceiptRequiredTasksComplete(scheduledTask.getResponsePeriod())) {
        scheduledTask.setActionState(ScheduledTaskState.COMPLETED);
        scheduledTaskRepository.saveAndFlush(scheduledTask);
        return;
      }

      processActionWithPackCode(scheduledTask, scheduledTaskDetails);
    }
  }

  private boolean areAllReceiptRequiredTasksComplete(ResponsePeriod responsePeriod) {
    // In the Spring time we enjoy nesting
    for (ScheduledTask scheduledTask : responsePeriod.getScheduledTasks()) {
      if (scheduledTask.isReceiptRequiredForCompletion()) {
        if (scheduledTask.getActionState() != ScheduledTaskState.COMPLETED) {
          if (scheduledTask.getActionState() != ScheduledTaskState.SENT) {
            log.with(scheduledTask)
                .warn("ScheduledTask Not in SENT or COMPLETE state, when checking for COMPLETION");

            /* we would want to understand this though? for audit purposes, it would require a logic failure somewhere */
            // By default we can't complain to Case person about this
            continue;
          }

          // TODO: Could if required change this to record all failed scheduledTasks and return
          // Like validation recording
          return false;
        }
      }
    }

    return true;
  }

  private void processActionWithPackCode(
      ScheduledTask scheduledTask, Map<String, String> scheduledTaskDetails) {
    Case caze = scheduledTask.getResponsePeriod().getCaze();

    // Not sure about the ID (no correlation ID, scheduledTaskId seems sensible?
    // Metadata?  hmmm  understand it's use better, using null for now
    fulfilmentService.processPrintFulfilment(
        caze,
        scheduledTaskDetails.get("packCode"),
        scheduledTask.getId(),
        "SCHEDULED_TASK",
        null,
        scheduledTask);

    scheduledTask.setActionState(ScheduledTaskState.IN_FULFILMENT);
    scheduledTaskRepository.saveAndFlush(scheduledTask);
  }
}
