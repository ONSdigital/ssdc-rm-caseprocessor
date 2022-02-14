package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class ResponsePeriodDTO {
  private String name;
  // Keep record of these, just to view?
  private DateOffSet dateOffSet;
  private List<ScheduledTaskDTO> scheduledTasks;
}
