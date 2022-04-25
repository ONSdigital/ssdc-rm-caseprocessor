package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class ScheduleTemplateTaskGroup {
  private String name;
  // Use OffSet Date thingy instead
  private DateOffSet dateOffsetFromTaskGroupStart;
  private List<ScheduleTemplateTask> scheduleTemplateTasks;
}
