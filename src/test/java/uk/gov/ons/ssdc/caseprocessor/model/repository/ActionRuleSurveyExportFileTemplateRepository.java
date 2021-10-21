package uk.gov.ons.ssdc.caseprocessor.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleSurveyExportFileTemplate;

import java.util.UUID;

@Component
@ActiveProfiles("test")
public interface ActionRuleSurveyExportFileTemplateRepository
    extends JpaRepository<ActionRuleSurveyExportFileTemplate, UUID> {}
