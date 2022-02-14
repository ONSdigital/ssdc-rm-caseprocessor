package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;

@Service
public class FulfilmentService {
  private final FulfilmentToProcessRepository fulfilmentToProcessRepository;
  private final FulfilmentSurveyExportFileTemplateRepository
      fulfilmentSurveyExportFileTemplateRepository;

  public FulfilmentService(
      FulfilmentToProcessRepository fulfilmentToProcessRepository,
      FulfilmentSurveyExportFileTemplateRepository fulfilmentSurveyExportFileTemplateRepository) {
    this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
    this.fulfilmentSurveyExportFileTemplateRepository =
        fulfilmentSurveyExportFileTemplateRepository;
  }

  public void processPrintFulfilment(
      Case caze, String packCode, UUID correlationId, String originatingUser, Object metaData) {
    ExportFileTemplate exportFileTemplate = getAllowedPrintTemplate(packCode, caze);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setExportFileTemplate(exportFileTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setCorrelationId(correlationId);
    fulfilmentToProcess.setOriginatingUser(originatingUser);
    fulfilmentToProcess.setUacMetadata(metaData);

    fulfilmentToProcessRepository.saveAndFlush(fulfilmentToProcess);
  }

  private ExportFileTemplate getAllowedPrintTemplate(String packCode, Case caze) {
    List<FulfilmentSurveyExportFileTemplate> allowedTemplates =
        fulfilmentSurveyExportFileTemplateRepository.findBySurvey(
            caze.getCollectionExercise().getSurvey());

    for (FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate : allowedTemplates) {
      if (fulfilmentSurveyExportFileTemplate
          .getExportFileTemplate()
          .getPackCode()
          .equals(packCode)) {
        return fulfilmentSurveyExportFileTemplate.getExportFileTemplate();
      }
    }

    throw new RuntimeException(
        String.format(
            "Pack code %s is not allowed as a fulfilment on survey %s",
            packCode, caze.getCollectionExercise().getSurvey().getName()));
  }
}
