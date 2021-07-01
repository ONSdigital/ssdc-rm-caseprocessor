package uk.gov.ons.ssdc.caseprocessor.service;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;

@Component
public class CaseToProcessProcessor {

  private final PrintProcessor printProcessor;

  public CaseToProcessProcessor(PrintProcessor printProcessor) {
    this.printProcessor = printProcessor;
  }

  public void process(CaseToProcess caseToProcess) {
    if (caseToProcess.getActionRule().getType() == ActionRuleType.PRINT) {
      printProcessor.processPrintRow(
          caseToProcess.getActionRule().getTemplate(),
          caseToProcess.getCaze(),
          caseToProcess.getBatchId(),
          caseToProcess.getBatchQuantity(),
          caseToProcess.getActionRule().getPackCode(),
          caseToProcess.getActionRule().getPrintSupplier());
    } else {
      throw new NotImplementedException("No implementation for other types of action rule yet");
    }
  }
}
