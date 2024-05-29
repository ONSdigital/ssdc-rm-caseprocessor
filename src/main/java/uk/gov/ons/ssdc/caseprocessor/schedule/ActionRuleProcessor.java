package uk.gov.ons.ssdc.caseprocessor.schedule;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleStatus;

import java.util.List;

@Component
public class ActionRuleProcessor {
  private final CaseClassifier caseClassifier;
  private final ActionRuleRepository actionRuleRepository;

  public ActionRuleProcessor(
      CaseClassifier caseClassifier,
      ActionRuleRepository actionRuleRepository) {
    this.caseClassifier = caseClassifier;
    this.actionRuleRepository = actionRuleRepository;
  }

  @Transactional(
      propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every action rule
  public void processTriggeredActionRule(ActionRule triggeredActionRule) {
    int casesSelected = caseClassifier.enqueueCasesForActionRule(triggeredActionRule);
    triggeredActionRule.setHasTriggered(true);
    triggeredActionRule.setSelectedCaseCount(casesSelected);
    actionRuleRepository.save(triggeredActionRule);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateCompletedProcessingActionRules() {
    List<ActionRule> completedProcessingActionRules =
        actionRuleRepository.findCompletedProcessing();

    for (ActionRule actionRule : completedProcessingActionRules) {
      actionRule.setActionRuleStatus(ActionRuleStatus.COMPLETED);
      actionRuleRepository.save(actionRule);
    }
  }
}
