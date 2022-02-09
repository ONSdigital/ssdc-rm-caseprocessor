package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
public class ResponsePeriodDTO {
  private String name;
  // Keep record of these, just to view?
  private DateOffSet dateOffSet;
  private List<ScheduledTaskDTO> scheduledTasks;
}
