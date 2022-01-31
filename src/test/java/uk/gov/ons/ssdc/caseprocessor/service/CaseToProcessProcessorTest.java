package uk.gov.ons.ssdc.caseprocessor.service;

import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;

@ExtendWith(MockitoExtension.class)
class CaseToProcessProcessorTest {
  @Mock private ExportFileProcessor exportFileProcessor;
  @Mock private DeactivateUacProcessor deactivateUacProcessor;
  @Mock private SmsProcessor smsProcessor;

  @InjectMocks CaseToProcessProcessor underTest;

  @Test
  void testProcessExportFileActionRule() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});
    exportFileTemplate.setPackCode("test pack code");
    exportFileTemplate.setExportFileDestination("test export file destination");

    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setType(ActionRuleType.EXPORT_FILE);
    actionRule.setExportFileTemplate(exportFileTemplate);

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(exportFileProcessor)
        .processExportFileRow(
            exportFileTemplate.getTemplate(),
            caze,
            caseToProcess.getBatchId(),
            caseToProcess.getBatchQuantity(),
            exportFileTemplate.getPackCode(),
            exportFileTemplate.getExportFileDestination(),
            actionRule.getId(),
            null,
            actionRule.getUacMetadata(),
            null);
  }

  @Test
  void testProcessDeactivateUacActionRule() {
    // Given
    Case caze = new Case();

    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setType(ActionRuleType.DEACTIVATE_UAC);

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(deactivateUacProcessor).process(caze, actionRule.getId());
  }

  @Test
  void testProcessSmsActionRule() {
    // Given
    Case caze = new Case();

    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setType(ActionRuleType.SMS);

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(smsProcessor).process(caze, actionRule);
  }
}
