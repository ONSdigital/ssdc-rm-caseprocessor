package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class CaseDto {
  private UUID caseId;
  private Boolean receiptReceived;
  private Map<String, String> caze;
}
