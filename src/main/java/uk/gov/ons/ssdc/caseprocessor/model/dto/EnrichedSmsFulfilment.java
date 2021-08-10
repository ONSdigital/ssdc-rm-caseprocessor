package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import javax.persistence.Id;
import lombok.Data;

@Data
public class EnrichedSmsFulfilment {
  private UUID caseId;
  private String telephoneNumber;
  private String packCode;
  private String uac;
  private String qid;
}
