package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRule;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;

public class ActionRuleTriggererTest {
  private final ActionRuleRepository actionRuleRepository = mock(ActionRuleRepository.class);
  private final ActionRuleProcessor actionRuleProcessor = mock(ActionRuleProcessor.class);

  @Test
  public void testTriggerActionRule() throws UnknownHostException {
    // Given
    ActionRule actionRule = new ActionRule();
    when(actionRuleRepository.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            any(OffsetDateTime.class)))
        .thenReturn(Collections.singletonList(actionRule));

    // When
    ActionRuleTriggerer underTest =
        new ActionRuleTriggerer(actionRuleRepository, actionRuleProcessor);
    underTest.triggerActionRule();

    // Then
    verify(actionRuleProcessor).processTriggeredActionRule(actionRule);
  }

  @Test
  public void testTriggerMultipleActionRule() throws UnknownHostException {
    // Given
    List<ActionRule> actionRules = new ArrayList<>(50);
    for (int i = 0; i < 50; i++) {
      actionRules.add(new ActionRule());
    }

    when(actionRuleRepository.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            any(OffsetDateTime.class)))
        .thenReturn(actionRules);

    // When
    ActionRuleTriggerer underTest =
        new ActionRuleTriggerer(actionRuleRepository, actionRuleProcessor);
    underTest.triggerActionRule();

    // Then
    verify(actionRuleProcessor, times(50)).processTriggeredActionRule(any(ActionRule.class));
  }
}
