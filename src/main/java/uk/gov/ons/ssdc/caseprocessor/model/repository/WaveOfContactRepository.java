package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;

public interface WaveOfContactRepository extends JpaRepository<WaveOfContact, UUID> {
  List<WaveOfContact> findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
      OffsetDateTime triggerDateTime);
}
