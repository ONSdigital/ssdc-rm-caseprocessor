package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {

  @Query(
      value =
          "SELECT * FROM casev3.scheduled_tasks where rm_to_action_date < NOW() LIMIT :limit FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  Stream<ScheduledTask> findScheduledTasks(@Param("limit") int limit);
}
