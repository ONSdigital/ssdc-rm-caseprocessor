package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import javax.persistence.Id;
import lombok.Data;

@Data
public class SmsFulfilment {

  @Id private UUID caseId;

  private String telephoneNumber;
}
