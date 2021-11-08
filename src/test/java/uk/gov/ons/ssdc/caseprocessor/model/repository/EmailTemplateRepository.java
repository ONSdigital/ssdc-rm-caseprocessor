package uk.gov.ons.ssdc.caseprocessor.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.common.model.entity.EmailTemplate;

@Component
@ActiveProfiles("test")
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {}
