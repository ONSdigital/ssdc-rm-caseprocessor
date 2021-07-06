package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRule;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;

public class ActionRuleProcessorTest {
  private final ActionRuleRepository actionRuleRepository = mock(ActionRuleRepository.class);
  private final CaseClassifier caseClassifier = mock(CaseClassifier.class);

  @Test
  public void testExecuteClassifiers() {
    // Given
    ActionRule actionRule = setUpActionRule(ActionRuleType.PRINT);

    // when
    ActionRuleProcessor underTest = new ActionRuleProcessor(caseClassifier, actionRuleRepository);
    underTest.triggerActionRule(actionRule);

    // then
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepository, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);

    verify(caseClassifier).enqueueCasesForActionRule(eq(actionRule));
  }

  private ActionRule setUpActionRule(ActionRuleType actionRuleType) {
    ActionRule actionRule = new ActionRule();
    UUID actionRuleId = UUID.randomUUID();
    actionRule.setId(actionRuleId);
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);

    actionRule.setType(actionRuleType);

    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());

    actionRule.setCollectionExercise(collectionExercise);

    return actionRule;
  }
}
