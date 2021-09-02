package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;

public interface ActionRuleRepository extends JpaRepository<ActionRule, UUID> {
  List<ActionRule> findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
      OffsetDateTime triggerDateTime);
}
