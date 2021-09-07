package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.PrintFileRow;

public interface PrintFileRowRepository extends JpaRepository<PrintFileRow, UUID> {}
