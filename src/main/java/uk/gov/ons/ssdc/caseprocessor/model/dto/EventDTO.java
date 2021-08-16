package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class EventDTO {
  private EventHeaderDTO header;
  private PayloadDTO payload;
}
