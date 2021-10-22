package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateSample {

  private UUID caseId;
  private Map<String, String> sample;
}
