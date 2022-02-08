package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

import java.time.temporal.ChronoUnit;

@Data
public class ScheduleTemplate {
  private String name;
  private ResponsePeriod[] responsePeriods;
}
