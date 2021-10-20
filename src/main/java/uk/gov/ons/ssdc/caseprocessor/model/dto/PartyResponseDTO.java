package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class PartyResponseDTO {
  private UUID id;
  private UUID sampleSummaryId;
}
