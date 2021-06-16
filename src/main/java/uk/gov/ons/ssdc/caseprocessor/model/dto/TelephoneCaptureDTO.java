package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class TelephoneCaptureDTO {

  private UUID caseId;

  private String uac;

  private String qid;
}
