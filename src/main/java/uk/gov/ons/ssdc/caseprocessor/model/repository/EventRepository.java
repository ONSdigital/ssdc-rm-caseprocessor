package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {

  void deleteByCaze_IdIn(List<UUID> cazeIds);
}
