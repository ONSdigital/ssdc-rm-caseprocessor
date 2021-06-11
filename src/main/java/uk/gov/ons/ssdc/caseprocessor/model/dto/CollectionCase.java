package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class CollectionCase {
  private UUID caseId;
  private Boolean receiptReceived;
  private Boolean invalidAddrress;
  private Boolean surveyLaunched;
  private RefusalTypeDTO refusalReceived;
  private Map<String, String> sample;
}
