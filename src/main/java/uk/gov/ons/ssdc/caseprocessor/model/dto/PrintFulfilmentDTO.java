package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class PrintFulfilmentDTO {
  private UUID caseId;
  private String packCode;
}
