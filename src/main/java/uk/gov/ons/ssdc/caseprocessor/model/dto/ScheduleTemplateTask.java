package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleTemplateTask {
  private String name;
  private ScheduledTaskType scheduledTaskType;
  private String packCode;
  private boolean receiptRequired;
  private DateOffSet dateOffSetFromStart;
}
