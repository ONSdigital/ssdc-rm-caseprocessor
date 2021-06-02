package uk.gov.ons.ssdc.caseprocessor.model.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {}
