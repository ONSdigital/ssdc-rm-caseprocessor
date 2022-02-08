package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.Data;

@Data
public class ResponsePeriod {
  private String name;
  // Use OffSet Date thingy instead
  private ChronoUnit dateUnit;
  private int offSetFromStart;
  private List<Task> tasks;
}
