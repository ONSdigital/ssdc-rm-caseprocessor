package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class RasRmPartyAssociationDTO {
  private String businessRespondentStatus; // TODO: could be an enum, but we only check one status
}
