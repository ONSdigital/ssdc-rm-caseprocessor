package uk.gov.ons.ssdc.caseprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

@Component
public class ScheduledTaskService {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();
  private final ScheduledTaskRepository scheduledTaskRepository;
  private final CaseService caseService;

  public ScheduledTaskService(
      ScheduledTaskRepository scheduledTaskRepository, CaseService caseService) {
    this.scheduledTaskRepository = scheduledTaskRepository;
    this.caseService = caseService;
  }

  public void createScheduledTasksFromSchedulePlan(
      List<CaseScheduledTaskGroup> caseScheduledTaskGroups, UUID caseId) {
    for (CaseScheduledTaskGroup caseScheduledTaskGroup : caseScheduledTaskGroups) {
      for (CaseScheduledTask caseScheduledTask : caseScheduledTaskGroup.getScheduledTasks()) {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setId(caseScheduledTask.getId());
        scheduledTask.setName(caseScheduledTask.getName());
        scheduledTask.setRmToActionDate(caseScheduledTask.getRmScheduledDateTime());
        scheduledTask.setScheduledTaskType(caseScheduledTask.getScheduledTaskType());
        scheduledTask.setPackCode(caseScheduledTask.getPackCode());
        scheduledTask.setRmToActionDate(scheduledTask.getRmToActionDate());
        scheduledTask.setCaseId(caseId);

        scheduledTaskRepository.saveAndFlush(scheduledTask);
      }
    }
  }
}
