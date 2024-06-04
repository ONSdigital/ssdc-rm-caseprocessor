package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleStatus;

@Component
public class ActionRuleTriggerer {
  private static final Logger log = LoggerFactory.getLogger(ActionRuleTriggerer.class);
  private final ActionRuleRepository actionRuleRepository;
  private final ActionRuleProcessor actionRuleProcessor;

  private String hostName = InetAddress.getLocalHost().getHostName();

  public ActionRuleTriggerer(
      ActionRuleRepository actionRuleRepository, ActionRuleProcessor actionRuleProcessor)
      throws UnknownHostException {
    this.actionRuleRepository = actionRuleRepository;
    this.actionRuleProcessor = actionRuleProcessor;
  }

  @Transactional
  public void triggerActionRule() {
    List<ActionRule> triggeredActionRules =
        actionRuleRepository.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            OffsetDateTime.now());

    for (ActionRule triggeredActionRule : triggeredActionRules) {

      try {
        log.atInfo()
            .setMessage("Action rule selecting cases")
            .addKeyValue("hostName", hostName)
            .addKeyValue("id", triggeredActionRule.getId())
            .log();

        actionRuleProcessor.updateActionRuleStatus(
            triggeredActionRule, ActionRuleStatus.SELECTING_CASES);

        actionRuleProcessor.processTriggeredActionRule(triggeredActionRule);

        actionRuleProcessor.updateActionRuleStatus(
            triggeredActionRule, ActionRuleStatus.PROCESSING_CASES);
        log.atInfo()
            .setMessage("Action rule triggered")
            .addKeyValue("hostName", hostName)
            .addKeyValue("id", triggeredActionRule.getId())
            .log();

      } catch (BadSqlGrammarException badSqlGrammarException) {
        String errorMessage =
            "ActionRule "
                + triggeredActionRule.getId()
                + " failed with a BadSqlGrammarException,"
                + " it has been marked Triggered to stop it running until it is fixed."
                + " Exception Message: "
                + badSqlGrammarException.getMessage();
        log.atError()
            .setMessage(errorMessage)
            .addKeyValue("hostName", hostName)
            .addKeyValue("id", triggeredActionRule.getId())
            .log();

        // Doing this update here means it's part of the overall method transaction, but this
        // transaction wraps the entire loop including every action being triggered and won't be
        // commited until any other actions also triggering are finished
        // TODO re-write this method so that each action is handled in its own transaction
        triggeredActionRule.setActionRuleStatus(ActionRuleStatus.ERRORED);
        triggeredActionRule.setHasTriggered(true);
        actionRuleRepository.save(triggeredActionRule);

      } catch (Exception e) {
        log.atError()
            .setMessage("Unexpected error while executing action rule")
            .setCause(e)
            .addKeyValue("hostName", hostName)
            .addKeyValue("id", triggeredActionRule.getId())
            .log();
      }
    }
  }
}
