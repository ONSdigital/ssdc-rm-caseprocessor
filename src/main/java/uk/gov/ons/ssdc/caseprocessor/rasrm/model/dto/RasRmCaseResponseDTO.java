package uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class RasRmCaseResponseDTO {
  private UUID id;
  private RasRmCaseGroupDTO caseGroup;
}
