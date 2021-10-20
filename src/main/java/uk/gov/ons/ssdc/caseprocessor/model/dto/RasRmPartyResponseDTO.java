package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class RasRmPartyResponseDTO {
  private UUID id;
  private RasRmPartyAssociationDTO[] associations;
}
