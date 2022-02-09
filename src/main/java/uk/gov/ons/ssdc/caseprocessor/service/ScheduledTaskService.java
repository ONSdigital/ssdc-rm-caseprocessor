package uk.gov.ons.ssdc.caseprocessor.service;

import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponsePeriodDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduledTaskDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskStatus;

@Component
public class ScheduledTaskService {

  private final ScheduledTaskRepository scheduledTaskRepository;

  public ScheduledTaskService(ScheduledTaskRepository scheduledTaskRepository) {
    this.scheduledTaskRepository = scheduledTaskRepository;
  }

  public void createScheduledTasksFromSchedulePlan(ResponsePeriodDTO[] responsePeriodDTOS) {
    for (ResponsePeriodDTO responsePeriodDTO : responsePeriodDTOS) {
      for (ScheduledTaskDTO scheduledTaskDTO : responsePeriodDTO.getScheduledTasks()) {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setId(scheduledTaskDTO.getId());
        scheduledTask.setScheduledTaskStatus(ScheduledTaskStatus.NOT_STARTED);
        scheduledTask.setRmToActionDate(scheduledTask.getRmToActionDate());

        scheduledTaskRepository.saveAndFlush(scheduledTask);
      }
    }
  }
}
