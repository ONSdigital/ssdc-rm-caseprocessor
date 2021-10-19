package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class PartyDTO {
  private String sampleUnitRef;
  private String sampleUnitType;
  private UUID sampleSummaryId;
  private Map<String, String> attributes;
}
