package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class NotifyRequestTaskPayload {
  String email;
  String notifyTemplateId;
  String notifyServiceRef;
  Map<String, String> personalisation;
  UUID transactionId;
  String correlationId;
}
