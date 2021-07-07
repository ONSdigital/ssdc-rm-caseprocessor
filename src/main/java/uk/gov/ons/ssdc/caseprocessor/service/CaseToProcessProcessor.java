package uk.gov.ons.ssdc.caseprocessor.service;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.PrintTemplate;

@Component
public class CaseToProcessProcessor {

  private final PrintProcessor printProcessor;
  private final DeactivateUacProcessor deactivateUacProcessor;

  public CaseToProcessProcessor(
      PrintProcessor printProcessor, DeactivateUacProcessor deactivateUacProcessor) {
    this.printProcessor = printProcessor;
    this.deactivateUacProcessor = deactivateUacProcessor;
  }

  public void process(CaseToProcess caseToProcess) {
    ActionRuleType actionRuleType = caseToProcess.getActionRule().getType();
    if (actionRuleType == ActionRuleType.PRINT) {
      PrintTemplate printTemplate = caseToProcess.getActionRule().getPrintTemplate();
      printProcessor.processPrintRow(
          printTemplate.getTemplate(),
          caseToProcess.getCaze(),
          caseToProcess.getBatchId(),
          caseToProcess.getBatchQuantity(),
          printTemplate.getPackCode(),
          printTemplate.getPrintSupplier());
    } else if (actionRuleType == ActionRuleType.DEACTIVATE_UAC) {
      deactivateUacProcessor.process(caseToProcess.getCaze());
    } else {
      throw new NotImplementedException("No implementation for other types of action rule yet");
    }
  }
}
