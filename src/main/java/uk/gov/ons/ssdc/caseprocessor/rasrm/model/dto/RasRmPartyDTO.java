package uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class RasRmPartyDTO {
  private String sampleUnitRef;
  private String sampleUnitType;
  private UUID sampleSummaryId;
  private Map<String, Object> attributes;
}
