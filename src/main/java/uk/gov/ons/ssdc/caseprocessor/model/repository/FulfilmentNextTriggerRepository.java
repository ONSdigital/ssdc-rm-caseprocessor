package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentNextTrigger;

public interface FulfilmentNextTriggerRepository
    extends JpaRepository<FulfilmentNextTrigger, UUID> {
  Optional<FulfilmentNextTrigger> findByTriggerDateTimeBefore(OffsetDateTime triggerDateTime);
}
