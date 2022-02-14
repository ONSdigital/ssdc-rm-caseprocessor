package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class ScheduleTemplate {
  private String name;
  private ResponsePeriod[] responsePeriods;
}
