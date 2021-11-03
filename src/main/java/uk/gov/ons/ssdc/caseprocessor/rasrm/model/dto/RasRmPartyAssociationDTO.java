package uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto;

import lombok.Data;

@Data
public class RasRmPartyAssociationDTO {
  private String businessRespondentStatus; // Do not use enum because of unexpected values
}
