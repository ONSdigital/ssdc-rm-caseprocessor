package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleStatus;

@Component
public class ActionRuleProcessor {
  private static final Logger log = LoggerFactory.getLogger(ActionRuleProcessor.class);
  private final CaseClassifier caseClassifier;
  private final ActionRuleRepository actionRuleRepository;

  public ActionRuleProcessor(
      CaseClassifier caseClassifier, ActionRuleRepository actionRuleRepository) {
    this.caseClassifier = caseClassifier;
    this.actionRuleRepository = actionRuleRepository;
  }

  @Transactional(
      propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every action rule
  public void processTriggeredActionRule(ActionRule triggeredActionRule) {
    // NOTE: This function will block for the entire duration as the database filters the targeted
    // cases and creates cases to process rows
    LocalDateTime startTime = LocalDateTime.now();
    log.atTrace()
        .setMessage("Action rule selecting cases")
        .addKeyValue("id", triggeredActionRule.getId())
        .log();
    int casesSelected = caseClassifier.enqueueCasesForActionRule(triggeredActionRule);
    triggeredActionRule.setHasTriggered(true);
    triggeredActionRule.setSelectedCaseCount(casesSelected);
    triggeredActionRule.setActionRuleStatus(ActionRuleStatus.PROCESSING_CASES);
    actionRuleRepository.save(triggeredActionRule);
    LocalDateTime endTime = LocalDateTime.now();
    log.atTrace()
        .setMessage("Action rule selected cases")
        .addKeyValue("id", triggeredActionRule.getId())
        .addKeyValue("cases_selected", casesSelected)
        .addKeyValue("duration", endTime.minusNanos(startTime.getNano()).toString())
        .log();
  }

  @Transactional(
      propagation = Propagation.REQUIRES_NEW) // We need status updates to be committed immediately
  public ActionRule updateActionRuleStatus(
      ActionRule actionRule, ActionRuleStatus actionRuleStatus) {
    actionRule.setActionRuleStatus(actionRuleStatus);
    return actionRuleRepository.save(actionRule);
  }

  public void updateCompletedProcessingActionRules() {
    List<ActionRule> completedProcessingActionRules =
        actionRuleRepository.findCompletedProcessing();

    for (ActionRule actionRule : completedProcessingActionRules) {
      updateActionRuleStatus(actionRule, ActionRuleStatus.COMPLETED);
    }
  }
}
