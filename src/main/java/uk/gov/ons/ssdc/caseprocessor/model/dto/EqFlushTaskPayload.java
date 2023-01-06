package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class EqFlushTaskPayload implements CloudTaskPayload {
  private String qid;
  private UUID transactionId;
}
