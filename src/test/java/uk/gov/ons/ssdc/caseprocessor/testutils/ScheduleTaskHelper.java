package uk.gov.ons.ssdc.caseprocessor.testutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.temporal.ChronoUnit;
import java.util.List;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DateOffSet;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplateTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplateTaskGroup;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;

public class ScheduleTaskHelper {

  public static ScheduleTemplate createOneTaskSimpleScheduleTemplate() {
    ScheduleTemplateTask scheduleTemplateTask = new ScheduleTemplateTask();
    scheduleTemplateTask.setName("Task 1");
    scheduleTemplateTask.setScheduledTaskType(ScheduledTaskType.ACTION_WITH_PACKCODE);
    scheduleTemplateTask.setDateOffSetFromStart(new DateOffSet(ChronoUnit.DAYS, 1));
    scheduleTemplateTask.setPackCode("Test-Pack-Code");

    ScheduleTemplateTaskGroup scheduleTemplateTaskGroup = new ScheduleTemplateTaskGroup();
    scheduleTemplateTaskGroup.setName("Task Group 1");
    scheduleTemplateTaskGroup.setScheduleTemplateTasks(List.of(scheduleTemplateTask));
    scheduleTemplateTaskGroup.setDateOffsetFromTaskGroupStart((new DateOffSet(ChronoUnit.DAYS, 0)));

    ScheduleTemplateTaskGroup[] scheduleTemplateTaskGroups = new ScheduleTemplateTaskGroup[]{scheduleTemplateTaskGroup};

    ScheduleTemplate scheduleTemplate = new ScheduleTemplate();
    scheduleTemplate.setName("Simple Schedule Template");
    scheduleTemplate.setScheduleTemplateTaskGroups(scheduleTemplateTaskGroups);

    return scheduleTemplate;
  }

  public static String convertObjectToJson(Object obj) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed converting Object To Json", e);
    }
  }

}
