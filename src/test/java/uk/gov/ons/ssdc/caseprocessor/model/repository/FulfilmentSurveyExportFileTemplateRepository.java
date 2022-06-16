package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;

@Component
@ActiveProfiles("${spring.profiles.active}")
public interface FulfilmentSurveyExportFileTemplateRepository
    extends JpaRepository<FulfilmentSurveyExportFileTemplate, UUID> {}
