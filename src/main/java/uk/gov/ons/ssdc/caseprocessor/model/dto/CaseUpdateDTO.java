package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class CaseUpdateDTO {
  private UUID caseId;
  private boolean receiptReceived;
  private boolean invalid;
  private boolean surveyLaunched;
  private RefusalTypeDTO refusalReceived;
  private Map<String, String> sample;
}
