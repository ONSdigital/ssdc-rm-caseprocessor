package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class PrintRow {
  private String row;
  private UUID batchId;
  private int batchQuantity;
  private String printSupplier;
  private String packCode;
}
