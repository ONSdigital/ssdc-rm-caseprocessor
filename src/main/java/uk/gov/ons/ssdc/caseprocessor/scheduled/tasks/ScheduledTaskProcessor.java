package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.service.FulfilmentService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;

@Component
public class ScheduledTaskProcessor {

  private final ScheduledTaskRepository scheduledTaskRepository;
  private final FulfilmentService fulfilmentService;

  public ScheduledTaskProcessor(
      ScheduledTaskRepository scheduledTaskRepository, FulfilmentService fulfilmentService) {
    this.scheduledTaskRepository = scheduledTaskRepository;
    this.fulfilmentService = fulfilmentService;
  }

  public void process(ScheduledTask scheduledTask) {
    Map<String, String> scheduledTaskDetails = scheduledTask.getScheduledTaskDetails();
    String scheduledTaskType = scheduledTaskDetails.get("ScheduledTaskType");
    // In future we could unpack these into a variety of objects?
    // Some sort of switch etc.

    if (scheduledTaskType.equals(ScheduledTaskType.ACTION_WITH_PACKCODE.toString())) {
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
}
