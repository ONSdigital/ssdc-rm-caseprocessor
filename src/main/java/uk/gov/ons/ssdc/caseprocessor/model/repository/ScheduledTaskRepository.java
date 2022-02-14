package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {}
