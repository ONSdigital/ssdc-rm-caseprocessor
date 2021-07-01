package uk.gov.ons.ssdc.caseprocessor.schedule;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRule;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;

@Component
public class ActionRuleProcessor {
  private final CaseClassifier caseClassifier;
  private final ActionRuleRepository actionRuleRepository;

  public ActionRuleProcessor(
      CaseClassifier caseClassifier, ActionRuleRepository actionRuleRepository) {
    this.caseClassifier = caseClassifier;
    this.actionRuleRepository = actionRuleRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every WoC
  public void createScheduledActionRule(ActionRule triggeredActionRule) {
    caseClassifier.enqueueCasesForActionRule(triggeredActionRule);
    triggeredActionRule.setHasTriggered(true);
    actionRuleRepository.save(triggeredActionRule);
  }
}