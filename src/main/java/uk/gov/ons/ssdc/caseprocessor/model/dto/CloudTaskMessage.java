package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class CloudTaskMessage {
  private CloudTaskType cloudTaskType;
  private CloudTaskPayload payload;
}
