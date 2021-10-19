package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

@Data
public class SampleSummaryDTO {
  private int totalSampleUnits;
  private int expectedCollectionInstruments;
}
