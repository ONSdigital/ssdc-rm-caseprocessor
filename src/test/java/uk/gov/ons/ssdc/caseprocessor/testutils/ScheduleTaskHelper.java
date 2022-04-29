package uk.gov.ons.ssdc.caseprocessor.testutils;

import java.time.temporal.ChronoUnit;
import java.util.List;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.DateOffSet;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplate;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplateTask;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplateTaskGroup;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;

public class ScheduleTaskHelper {

  public static ScheduleTemplate createOneTaskSimpleScheduleTemplate(
      ChronoUnit taskChronoUnit, int taskOffset) {
    ScheduleTemplateTask scheduleTemplateTask = new ScheduleTemplateTask();
    scheduleTemplateTask.setName("Task 1");
    scheduleTemplateTask.setScheduledTaskType(ScheduledTaskType.ACTION_WITH_PACKCODE);
    scheduleTemplateTask.setDateOffSetFromStart(new DateOffSet(taskChronoUnit, taskOffset));
    scheduleTemplateTask.setPackCode("Test-Pack-Code");

    ScheduleTemplateTaskGroup scheduleTemplateTaskGroup = new ScheduleTemplateTaskGroup();
    scheduleTemplateTaskGroup.setName("Task Group 1");
    scheduleTemplateTaskGroup.setScheduleTemplateTasks(List.of(scheduleTemplateTask));
    scheduleTemplateTaskGroup.setDateOffsetFromTaskGroupStart((new DateOffSet(ChronoUnit.DAYS, 0)));

    List<ScheduleTemplateTaskGroup> scheduleTemplateTaskGroups = List.of(scheduleTemplateTaskGroup);

    ScheduleTemplate scheduleTemplate = new ScheduleTemplate();
    scheduleTemplate.setName("Simple Schedule Template");
    scheduleTemplate.setScheduleTemplateTaskGroups(scheduleTemplateTaskGroups);

    return scheduleTemplate;
  }
}
