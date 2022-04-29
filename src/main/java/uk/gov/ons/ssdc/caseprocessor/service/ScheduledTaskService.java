package uk.gov.ons.ssdc.caseprocessor.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplate;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplateTask;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplateTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

@Component
public class ScheduledTaskService {
  private final ScheduledTaskRepository scheduledTaskRepository;

  public ScheduledTaskService(ScheduledTaskRepository scheduledTaskRepository) {
    this.scheduledTaskRepository = scheduledTaskRepository;
  }

  public List<CaseScheduledTaskGroup> addScheduleToDBAndReturnScheduledTaskGroups(Case caze) {

    ScheduleTemplate scheduleTemplate =
        caze.getCollectionExercise().getSurvey().getScheduleTemplate();

    return createScheduledTaskGroups(scheduleTemplate, caze.getId());
  }

  private List<CaseScheduledTaskGroup> createScheduledTaskGroups(
      ScheduleTemplate scheduleTemplate, UUID caseId) {

    List<CaseScheduledTaskGroup> caseScheduledTaskGroups = new ArrayList<>();
    OffsetDateTime caseTaskGroupStartDate = OffsetDateTime.now();

    for (ScheduleTemplateTaskGroup scheduleTemplateTaskGroup :
        scheduleTemplate.getScheduleTemplateTaskGroups()) {
      CaseScheduledTaskGroup caseScheduledTaskGroup = new CaseScheduledTaskGroup();
      caseScheduledTaskGroup.setName(scheduleTemplateTaskGroup.getName());

      caseScheduledTaskGroup.setDateOffsetFromTaskGroupStart(
          scheduleTemplateTaskGroup.getDateOffsetFromTaskGroupStart());

      caseScheduledTaskGroups.add(caseScheduledTaskGroup);

      caseTaskGroupStartDate =
          caseTaskGroupStartDate.plusSeconds(
              caseScheduledTaskGroup
                      .getDateOffsetFromTaskGroupStart()
                      .getDateUnit()
                      .getDuration()
                      .getSeconds()
                  * caseScheduledTaskGroup.getDateOffsetFromTaskGroupStart().getOffset());

      caseScheduledTaskGroup.setScheduledTasks(
          createScheduledTaskList(scheduleTemplateTaskGroup, caseTaskGroupStartDate, caseId));
    }

    return caseScheduledTaskGroups;
  }

  private List<CaseScheduledTask> createScheduledTaskList(
      ScheduleTemplateTaskGroup scheduleTemplateTaskGroup, OffsetDateTime startDate, UUID caseId) {
    List<CaseScheduledTask> caseScheduledTaskList = new ArrayList<>();

    for (ScheduleTemplateTask scheduleTemplateTask :
        scheduleTemplateTaskGroup.getScheduleTemplateTasks()) {
      CaseScheduledTask caseScheduledTask = new CaseScheduledTask();
      caseScheduledTask.setId(UUID.randomUUID());
      caseScheduledTask.setName(scheduleTemplateTask.getName());
      caseScheduledTask.setScheduledTaskType(scheduleTemplateTask.getScheduledTaskType());
      caseScheduledTask.setPackCode(scheduleTemplateTask.getPackCode());

      OffsetDateTime scheduledTaskRunDateTime =
          startDate.plusSeconds(
              scheduleTemplateTask.getDateOffSetFromStart().getDateUnit().getDuration().getSeconds()
                  * scheduleTemplateTask.getDateOffSetFromStart().getOffset());

      caseScheduledTask.setScheduledDateToRun(scheduledTaskRunDateTime.toString());

      caseScheduledTask.setDateOffSetFromStart(scheduleTemplateTask.getDateOffSetFromStart());
      caseScheduledTask.setEventIds(new ArrayList<>());
      caseScheduledTask.setUacsIds(new ArrayList<>());

      caseScheduledTaskList.add(caseScheduledTask);

      addScheduledTaskToDatabase(caseScheduledTask, scheduledTaskRunDateTime, caseId);
    }

    return caseScheduledTaskList;
  }

  private void addScheduledTaskToDatabase(
      CaseScheduledTask caseScheduledTask, OffsetDateTime scheduledTaskRunDateTime, UUID caseId) {

    ScheduledTask scheduledTask = new ScheduledTask();
    scheduledTask.setId(caseScheduledTask.getId());
    scheduledTask.setName(caseScheduledTask.getName());
    scheduledTask.setRmToActionDate(scheduledTaskRunDateTime);
    scheduledTask.setScheduledTaskType(caseScheduledTask.getScheduledTaskType());
    scheduledTask.setPackCode(caseScheduledTask.getPackCode());
    scheduledTask.setRmToActionDate(scheduledTask.getRmToActionDate());
    scheduledTask.setCaseId(caseId);

    scheduledTaskRepository.saveAndFlush(scheduledTask);
  }
}
