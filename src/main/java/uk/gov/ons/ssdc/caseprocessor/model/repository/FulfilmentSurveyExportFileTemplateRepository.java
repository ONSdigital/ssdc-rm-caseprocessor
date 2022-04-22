package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.Survey;

public interface FulfilmentSurveyExportFileTemplateRepository
    extends JpaRepository<FulfilmentSurveyExportFileTemplate, UUID> {
  List<FulfilmentSurveyExportFileTemplate> findBySurvey(Survey survey);
}
