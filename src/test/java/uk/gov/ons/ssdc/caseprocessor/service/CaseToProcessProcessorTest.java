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

@RunWith(MockitoJUnitRunner.class)
public class CaseToProcessProcessorTest {
  @Mock private PrintProcessor printProcessor;

  @InjectMocks CaseToProcessProcessor underTest;

  @Test
  public void testProccessPrintActionRule() {
    // Given
    Case caze = new Case();
    caze.setSample(Map.of("foo", "bar"));
    caze.setCaseRef(123L);

    ActionRule actionRule = new ActionRule();
    actionRule.setType(ActionRuleType.PRINT);
    actionRule.setTemplate(new String[] {"__caseref__", "__uac__", "foo"});
    actionRule.setPackCode("test pack code");
    actionRule.setPrintSupplier("test print supplier");

    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(printProcessor)
        .processPrintRow(
            actionRule.getTemplate(),
            caze,
            caseToProcess.getBatchId(),
            caseToProcess.getBatchQuantity(),
            caseToProcess.getActionRule().getPackCode(),
            caseToProcess.getActionRule().getPrintSupplier());
  }
}
