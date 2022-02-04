package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DateOffSet {
  private DateUnit dateUnit;
  private int multiplier;
}
