package uk.gov.ons.ssdc.caseprocessor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponsePeriodDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduledTaskDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskStatus;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

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
        scheduledTask.setScheduledTaskStatus(ScheduledTaskStatus.NOT_STARTED);
        scheduledTask.setRmToActionDate(scheduledTask.getRmToActionDate());
        scheduledTask.setCaseId(caseId);

        scheduledTaskRepository.saveAndFlush(scheduledTask);
      }
    }
  }

  /*
   STATE

   OK. On the DB have triggered t/f

   Then it should be either be:
   OPEN (waiting for receipt)
   CLOSED (either no receipt required or receipted - or by some other mechanism).

   As off the DB we do not need to manage the state to clear table etc.  For example they could stay OPEN / never closed.  Doesn't hurt

  */

  public void updateScheduledTaskAgainstCase(
      Case caze,
      UUID scheduledTaskId,
      Event newEvent,
      UacQidLink newUacLink,
      ScheduledTaskStatus newStatus) {

    List<ResponsePeriodDTO> responsePeriodList;

    Map<String, String> madMap = (Map<String, String>) caze.getSchedule();

    try {
      String json = madMap.get("value");
      responsePeriodList = Arrays.asList(objectMapper.readValue(json, ResponsePeriodDTO[].class));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to map out scheduled: " + caze.getSchedule().toString());
    }

    for (ResponsePeriodDTO responsePeriodDTO : responsePeriodList) {
      for (ScheduledTaskDTO scheduledTaskDTO : responsePeriodDTO.getScheduledTasks()) {
        if (scheduledTaskDTO.getId().equals(scheduledTaskId)) {
          if (newEvent != null) {
            scheduledTaskDTO.getEventIds().add(newEvent.getId());
          }

          if (newUacLink != null) {
            scheduledTaskDTO.getUacsIds().add(newUacLink.getId());
          }

          if (newStatus != null) {
            scheduledTaskDTO.setScheduledTaskStatus(newStatus);
          }
        }
      }
    }

    PGobject jsonObject = new PGobject();
    jsonObject.setType("json");
    try {
      jsonObject.setValue(objectMapper.writeValueAsString(responsePeriodList));
    } catch (SQLException e) {
      throw new RuntimeException(e.getMessage());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }

    caze.setSchedule(jsonObject);

    caseService.saveCase(caze);
  }
  //
  //  public ScheduledTask getById(UUID scheduledTaskId) {
  //    Optional<ScheduledTask> scheduledTaskResult =
  // scheduledTaskRepository.findById(scheduledTaskId);
  //
  //    if (scheduledTaskResult.isEmpty()) {
  //      throw new RuntimeException(
  //          String.format("ScheduledTask with ID '%s' not found", scheduledTaskId));
  //    }
  //
  //    return scheduledTaskResult.get();
  //  }
}
