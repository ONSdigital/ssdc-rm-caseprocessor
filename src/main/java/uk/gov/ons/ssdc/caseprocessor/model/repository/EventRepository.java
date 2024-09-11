package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {

  void deleteByCazeId(UUID caseId);
}
