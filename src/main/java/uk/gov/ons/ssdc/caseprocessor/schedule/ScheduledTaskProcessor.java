package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.FulfilmentService;
import uk.gov.ons.ssdc.caseprocessor.service.ScheduledTaskService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskStatus;

@Component
public class ScheduledTaskProcessor {
  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskProcessor.class);

  private final FulfilmentService fulfilmentService;
  private final CaseService caseService;
  private final ScheduledTaskService scheduledTaskService;

  public ScheduledTaskProcessor(
      FulfilmentService fulfilmentService,
      CaseService caseService,
      ScheduledTaskService scheduledTaskService) {
    this.fulfilmentService = fulfilmentService;
    this.caseService = caseService;
    this.scheduledTaskService = scheduledTaskService;
  }

  public void process(ScheduledTask scheduledTask) {
    switch (scheduledTask.getScheduledTaskType()) {
      case ACTION_WITH_PACKCODE:
        processActionWithPackCode(scheduledTask);
        break;

      case COMPLETION:
        processsCompletionCheck(scheduledTask);
        break;

      default:
        {
          throw new RuntimeException(
              "Scheduled Task Type unknown: " + scheduledTask.getScheduledTaskType());
        }
    }
  }

  private void processsCompletionCheck(ScheduledTask scheduledTask) {

    // Needs re implemented
    //    if (scheduledTaskDetails.get("type").equals("COMPLETION")) {
    //      if (areAllReceiptRequiredTasksComplete(scheduledTask.getResponsePeriod())) {
    //        scheduledTask.setActionState(ScheduledTaskState.COMPLETED);
    //        scheduledTaskRepository.saveAndFlush(scheduledTask);
    //        return;
    //      }
    //
    //      processActionWithPackCode(scheduledTask, scheduledTaskDetails);
    //    }
  }

  //  private boolean areAllReceiptRequiredTasksComplete(ResponsePeriod responsePeriod) {
  //    // In the Spring time we enjoy nesting
  //    for (ScheduledTask scheduledTask : responsePeriod.getScheduledTasks()) {
  //      if (scheduledTask.isReceiptRequiredForCompletion()) {
  //        if (scheduledTask.getActionState() != ScheduledTaskState.COMPLETED) {
  //          if (scheduledTask.getActionState() != ScheduledTaskState.SENT) {
  //            log.with(scheduledTask)
  //                .warn("ScheduledTask Not in SENT or COMPLETE state, when checking for
  // COMPLETION");
  //
  //            /* we would want to understand this though? for audit purposes, it would require a
  // logic failure somewhere */
  //            // By default we can't complain to Case person about this
  //            continue;
  //          }
  //
  //          // TODO: Could if required change this to record all failed scheduledTasks and return
  //          // Like validation recording
  //          return false;
  //        }
  //      }
  //    }
  //
  //    return true;
  //  }

  private void processActionWithPackCode(ScheduledTask scheduledTask) {
    Case caze = caseService.getCase(scheduledTask.getCaseId());

    Map<String, UUID> metaData = Map.of("scheduledTaskId", scheduledTask.getId());

    fulfilmentService.processPrintFulfilment(
        caze,
        scheduledTask.getPackCode(),
        scheduledTask
            .getId(), // This is correlationId, use this for now - will link back to something at
        // least?
        "SRM_SCHEDULED_TASK",
        metaData);

    scheduledTaskService.updateScheduledTaskAgainstCase(
        caze, scheduledTask.getId(), null, null, ScheduledTaskStatus.IN_FULFILMENT);
  }
}
