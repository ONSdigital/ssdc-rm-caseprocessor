package uk.gov.ons.ssdc.caseprocessor.service;

import static uk.gov.ons.ssdc.caseprocessor.utils.PersonalisationTemplateHelper.doesTemplateRequireNewUacQid;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.client.UacQidServiceClient;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidCreatedPayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyEmailTemplateRepository;
import uk.gov.ons.ssdc.common.model.entity.EmailTemplate;
import uk.gov.ons.ssdc.common.model.entity.Survey;
import uk.gov.ons.ssdc.common.validation.EmailRule;

@Service
public class EmailRequestService {

  private final UacQidServiceClient uacQidServiceClient;
  private final FulfilmentSurveyEmailTemplateRepository fulfilmentSurveyEmailTemplateRepository;

  private final EmailRule emailValidationRule = new EmailRule(true);

  public EmailRequestService(
      UacQidServiceClient uacQidServiceClient,
      FulfilmentSurveyEmailTemplateRepository fulfilmentSurveyEmailTemplateRepository) {
    this.uacQidServiceClient = uacQidServiceClient;
    this.fulfilmentSurveyEmailTemplateRepository = fulfilmentSurveyEmailTemplateRepository;
  }

  public Optional<UacQidCreatedPayloadDTO> fetchNewUacQidPairIfRequired(String[] emailTemplate) {
    if (doesTemplateRequireNewUacQid(emailTemplate)) {
      return Optional.of(uacQidServiceClient.generateUacQid());
    }
    return Optional.empty();
  }

  public boolean isEmailTemplateAllowedOnSurvey(EmailTemplate emailTemplate, Survey survey) {
    return fulfilmentSurveyEmailTemplateRepository.existsByEmailTemplateAndSurvey(
        emailTemplate, survey);
  }

  public Optional<String> validateEmailAddress(String emailAddress) {
    return emailValidationRule.checkValidity(emailAddress);
  }
}
