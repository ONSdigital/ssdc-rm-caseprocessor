package uk.gov.ons.ssdc.caseprocessor.service;

import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;

@Component
public class CaseToProcessProcessor {

  private final PrintProcessor printProcessor;

  public CaseToProcessProcessor(PrintProcessor printProcessor) {
    this.printProcessor = printProcessor;
  }

  public void process(CaseToProcess caseToProcess) {
    if (caseToProcess.getWaveOfContact().getType() == WaveOfContactType.PRINT) {
      printProcessor.processPrintRow(caseToProcess.getWaveOfContact().getTemplate(),
              caseToProcess.getCaze(),
              caseToProcess.getBatchId(),
              caseToProcess.getBatchQuantity(),
              caseToProcess.getWaveOfContact().getPackCode(),
              caseToProcess.getWaveOfContact().getPrintSupplier());
    }
  }
}
