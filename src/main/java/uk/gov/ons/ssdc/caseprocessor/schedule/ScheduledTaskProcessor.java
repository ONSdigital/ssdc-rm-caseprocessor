package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.FulfilmentService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

@Component
public class ScheduledTaskProcessor {
  private final FulfilmentService fulfilmentService;
  private final CaseService caseService;

  public ScheduledTaskProcessor(FulfilmentService fulfilmentService, CaseService caseService) {
    this.fulfilmentService = fulfilmentService;
    this.caseService = caseService;
  }

  public void process(ScheduledTask scheduledTask) {
    switch (scheduledTask.getScheduledTaskType()) {
      case ACTION_WITH_PACKCODE:
        processActionWithPackCode(scheduledTask);
        break;

        // NEW TYPES GO HERE

      default:
        {
          throw new RuntimeException(
              "Scheduled Task Type unknown: " + scheduledTask.getScheduledTaskType());
        }
    }
  }

  private void processActionWithPackCode(ScheduledTask scheduledTask) {
    Case caze = caseService.getCase(scheduledTask.getCaseId());

    Map<String, UUID> metaData = Map.of("scheduledTaskId", scheduledTask.getId());

    fulfilmentService.processPrintFulfilment(
        caze,
        scheduledTask.getPackCode(),
        scheduledTask.getId(),
        "SRM_SCHEDULED_TASK",
        metaData,
        scheduledTask.getId());
  }
}
