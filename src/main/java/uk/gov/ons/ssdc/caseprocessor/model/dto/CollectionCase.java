package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class CollectionCase {
  private UUID caseId;
  private boolean receiptReceived;
  private boolean invalidAddress;
  private boolean surveyLaunched;
  private RefusalTypeDTO refusalReceived;
  private Map<String, String> sample;
}
