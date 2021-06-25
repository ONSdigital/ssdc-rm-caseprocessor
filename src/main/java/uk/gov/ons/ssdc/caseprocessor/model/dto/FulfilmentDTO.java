package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class FulfilmentDTO {
  private UUID caseId;
  private String fulfilmentCode;
}
