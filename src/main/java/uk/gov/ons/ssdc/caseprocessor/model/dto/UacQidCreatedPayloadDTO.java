package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class UacQidCreatedPayloadDTO {
  private String uac;
  private String qid;
}
