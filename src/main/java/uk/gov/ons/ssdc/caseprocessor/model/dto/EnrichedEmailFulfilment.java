package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class EnrichedEmailFulfilment {
  private UUID caseId;
  private String packCode;
  private String uac;
  private String qid;
  private Object uacMetadata;
}