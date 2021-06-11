package uk.gov.ons.ssdc.caseprocessor.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.UUID;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class UacDTO {
  private String uac;
  private Boolean active;
  private String questionnaireId;
  private UUID caseId;
  private UUID collectionExerciseId;
}
