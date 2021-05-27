package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class Peek {
  private String messageHash;
  private byte[] messagePayload;
}
