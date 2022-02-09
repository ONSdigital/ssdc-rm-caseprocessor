package uk.gov.ons.ssdc.caseprocessor.model.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

import java.util.UUID;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {

}
