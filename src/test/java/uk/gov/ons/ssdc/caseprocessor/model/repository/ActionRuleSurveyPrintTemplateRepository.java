package uk.gov.ons.ssdc.caseprocessor.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRuleSurveyPrintTemplate;

@Component
@ActiveProfiles("test")
public interface ActionRuleSurveyPrintTemplateRepository
    extends JpaRepository<ActionRuleSurveyPrintTemplate, UUID> {}
