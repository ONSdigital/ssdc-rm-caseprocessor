package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.List;
import lombok.Data;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.DateOffSet;

@Data
public class CaseScheduledTaskGroup {
  private String name;
  // Keep record of these, just to view?
  private DateOffSet dateOffsetFromTaskGroupStart;
  private List<CaseScheduledTask> scheduledTasks;
}
