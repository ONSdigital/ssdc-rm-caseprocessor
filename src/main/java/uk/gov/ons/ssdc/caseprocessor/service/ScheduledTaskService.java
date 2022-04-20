package uk.gov.ons.ssdc.caseprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponsePeriodDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduledTaskDTO;
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
      List<ResponsePeriodDTO> responsePeriodDTOS, UUID caseId) {
    for (ResponsePeriodDTO responsePeriodDTO : responsePeriodDTOS) {
      for (ScheduledTaskDTO scheduledTaskDTO : responsePeriodDTO.getScheduledTasks()) {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setId(scheduledTaskDTO.getId());
        scheduledTask.setName(scheduledTaskDTO.getName());
        scheduledTask.setRmToActionDate(scheduledTaskDTO.getRmScheduledDateTime());
        scheduledTask.setScheduledTaskType(scheduledTaskDTO.getScheduledTaskType());
        scheduledTask.setPackCode(scheduledTaskDTO.getPackCode());
        scheduledTask.setRmToActionDate(scheduledTask.getRmToActionDate());
        scheduledTask.setCaseId(caseId);

        scheduledTaskRepository.saveAndFlush(scheduledTask);
      }
    }
  }
}
