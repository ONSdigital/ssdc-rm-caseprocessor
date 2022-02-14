package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class ResponsePeriod {
  private String name;
  // Use OffSet Date thingy instead
  private DateOffSet dateOffSet;
  private List<Task> tasks;
}
