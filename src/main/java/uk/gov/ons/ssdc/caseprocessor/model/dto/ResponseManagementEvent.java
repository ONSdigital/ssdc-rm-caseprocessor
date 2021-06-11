package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class ResponseManagementEvent {
  private EventDTO event;
  private PayloadDTO payload;
}
