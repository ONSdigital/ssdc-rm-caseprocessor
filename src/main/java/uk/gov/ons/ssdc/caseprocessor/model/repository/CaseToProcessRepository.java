package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.ons.ssdc.common.model.entity.CaseToProcess;

public interface CaseToProcessRepository extends JpaRepository<CaseToProcess, UUID> {
  // To be deleted might be null so can't just check if it's false
  @Query(
      value =
          "SELECT * FROM casev3.case_to_process WHERE to_be_deleted IS NULL OR to_be_deleted = FALSE LIMIT :limit FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  Stream<CaseToProcess> findChunkToProcess(@Param("limit") int limit);

  // Queries could be merged with a param ... but I'm avoiding editing existing code that calls the
  // query for the spike
  @Query(
      value =
          "SELECT * FROM casev3.case_to_process WHERE to_be_deleted = TRUE LIMIT :limit FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  List<CaseToProcess> findChunkToDelete(@Param("limit") int limit);

  void deleteByCaze_IdIn(List<UUID> cazeIds);
}
