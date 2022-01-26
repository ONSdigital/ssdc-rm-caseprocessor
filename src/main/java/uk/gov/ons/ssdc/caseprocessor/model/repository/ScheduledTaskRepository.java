package uk.gov.ons.ssdc.caseprocessor.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {
  List<ScheduledTask> findByrmToActionDateTimeBeforeAndScheduledTaskStateEquals(
      OffsetDateTime now, ScheduledTaskState notStarted);
}
