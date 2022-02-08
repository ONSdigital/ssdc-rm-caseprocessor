package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DateOffSet {
  private ChronoUnit dateUnit;
  private int multiplier;
}
