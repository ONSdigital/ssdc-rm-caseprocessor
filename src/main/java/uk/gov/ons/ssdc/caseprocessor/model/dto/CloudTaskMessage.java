package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class CloudTaskMessage {
  private CloudTaskType cloudTaskType;
  private UUID correlationId;
  private CloudTaskPayload payload;
}
