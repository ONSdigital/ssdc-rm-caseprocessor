package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;

public interface UacQidLinkRepository extends JpaRepository<UacQidLink, UUID> {}
