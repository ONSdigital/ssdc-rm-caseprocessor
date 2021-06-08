package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Survey;

@RepositoryRestResource(collectionResourceRel = "surveys", path = "surveys")
public interface SurveyRepository extends JpaRepository<Survey, UUID> {}
