package uk.gov.ons.ssdc.caseprocessor.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ssdc.caseprocessor.model.entity.SmsTemplate;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, String> {}
