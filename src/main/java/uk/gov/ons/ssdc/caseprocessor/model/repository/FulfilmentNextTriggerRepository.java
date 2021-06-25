package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentNextTrigger;

@RepositoryRestResource(exported = false)
public interface FulfilmentNextTriggerRepository
    extends JpaRepository<FulfilmentNextTrigger, UUID> {
  Optional<FulfilmentNextTrigger> findByTriggerDateTimeBefore(OffsetDateTime triggerDateTime);
}
