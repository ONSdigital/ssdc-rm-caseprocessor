package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class InvalidAddress {
  private String reason;
  private String notes;
  private UUID caseId;
}
