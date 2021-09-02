package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;

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
        log.with("hostName", hostName)
            .with("id", triggeredActionRule.getId())
            .info("Action rule triggered");
        actionRuleProcessor.processTriggeredActionRule(triggeredActionRule);
      } catch (Exception e) {
        log.with("id", triggeredActionRule.getId())
            .error("Unexpected error while executing action rule - is classifier valid SQL?", e);
      }
    }
  }
}
