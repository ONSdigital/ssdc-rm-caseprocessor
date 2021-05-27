package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class ExceptionReportResponse {
  private boolean peek;
  private boolean logIt;
  private boolean skipIt;
}
