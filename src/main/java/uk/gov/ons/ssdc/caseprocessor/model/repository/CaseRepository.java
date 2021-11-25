package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.ons.ssdc.common.model.entity.Case;

public interface CaseRepository extends JpaRepository<Case, UUID> {
  @Query(
      value = "SELECT * FROM casev3.cases WHERE id = :id FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  Optional<Case> findByIdWithUpdateLock(@Param("id") UUID id);
}
