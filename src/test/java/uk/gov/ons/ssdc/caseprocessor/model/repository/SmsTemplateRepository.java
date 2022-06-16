package uk.gov.ons.ssdc.caseprocessor.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.common.model.entity.SmsTemplate;

@Component
@ActiveProfiles("${spring.profiles.active}")
public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, String> {}
