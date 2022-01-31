package uk.gov.ons.ssdc.caseprocessor.service;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.common.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;

@Component
public class CaseToProcessProcessor {
  private final ExportFileProcessor exportFileProcessor;
  private final DeactivateUacProcessor deactivateUacProcessor;
  private final SmsProcessor smsProcessor;
  private final EmailProcessor emailProcessor;

  public CaseToProcessProcessor(
      ExportFileProcessor exportFileProcessor,
      DeactivateUacProcessor deactivateUacProcessor,
      SmsProcessor smsProcessor,
      EmailProcessor emailProcessor) {
    this.exportFileProcessor = exportFileProcessor;
    this.deactivateUacProcessor = deactivateUacProcessor;
    this.smsProcessor = smsProcessor;
    this.emailProcessor = emailProcessor;
  }

  public void process(CaseToProcess caseToProcess) {
    ActionRuleType actionRuleType = caseToProcess.getActionRule().getType();
    switch (actionRuleType) {
      case EXPORT_FILE:
        ExportFileTemplate exportFileTemplate =
            caseToProcess.getActionRule().getExportFileTemplate();
        exportFileProcessor.processExportFileRow(
            exportFileTemplate.getTemplate(),
            caseToProcess.getCaze(),
            caseToProcess.getBatchId(),
            caseToProcess.getBatchQuantity(),
            exportFileTemplate.getPackCode(),
            exportFileTemplate.getExportFileDestination(),
            caseToProcess.getActionRule().getId(),
            null,
            caseToProcess.getActionRule().getUacMetadata(),
            null);
        break;
      case DEACTIVATE_UAC:
        deactivateUacProcessor.process(
            caseToProcess.getCaze(), caseToProcess.getActionRule().getId());
        break;
      case SMS:
        smsProcessor.process(caseToProcess.getCaze(), caseToProcess.getActionRule());
        break;
      case EMAIL:
        emailProcessor.process(caseToProcess.getCaze(), caseToProcess.getActionRule());
        break;
      default:
        throw new NotImplementedException("No implementation for other types of action rule yet");
    }
  }
}
