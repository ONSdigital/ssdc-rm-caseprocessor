package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TelephoneCaptureDTO {

  private UUID caseId;

  private String uac;

  private String qid;

  private Object uacMetadata;
}
