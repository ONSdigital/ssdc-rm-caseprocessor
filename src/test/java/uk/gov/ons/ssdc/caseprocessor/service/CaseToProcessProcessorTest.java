package uk.gov.ons.ssdc.caseprocessor.service;

import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRule;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.PrintTemplate;

@RunWith(MockitoJUnitRunner.class)
public class CaseToProcessProcessorTest {
  @Mock private PrintProcessor printProcessor;
  @Mock private DeactivateUacProcessor deactivateUacProcessor;

  @InjectMocks CaseToProcessProcessor underTest;

  @Test
  public void testProccessPrintActionRule() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    PrintTemplate printTemplate = new PrintTemplate();
    printTemplate.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});
    printTemplate.setPackCode("test pack code");
    printTemplate.setPrintSupplier("test print supplier");

    ActionRule actionRule = new ActionRule();
    actionRule.setType(ActionRuleType.PRINT);
    actionRule.setPrintTemplate(printTemplate);

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(printProcessor)
        .processPrintRow(
            printTemplate.getTemplate(),
            caze,
            caseToProcess.getBatchId(),
            caseToProcess.getBatchQuantity(),
            printTemplate.getPackCode(),
            printTemplate.getPrintSupplier());
  }

  @Test
  public void testProcessDeactivateUacActionRule() {
    // Given
    Case caze = new Case();

    ActionRule actionRule = new ActionRule();
    actionRule.setType(ActionRuleType.DEACTIVATE_UAC);

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(deactivateUacProcessor).process(caze);
  }
}
