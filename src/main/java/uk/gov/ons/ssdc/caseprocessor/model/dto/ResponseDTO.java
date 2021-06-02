package uk.gov.ons.ssdc.caseprocessor.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ResponseDTO {
  private String questionnaireId;

  @JsonProperty("dateTime")
  private OffsetDateTime responseDateTime;
}
