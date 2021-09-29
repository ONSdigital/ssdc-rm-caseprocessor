package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class SmsRequest {
  private UUID caseId;

  private String phoneNumber;

  private String packCode;

  private Map<String, String> uacMetadata;
}
