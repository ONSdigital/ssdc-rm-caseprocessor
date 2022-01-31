package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.Survey;

@Service
public class FulfilmentService {
  private final FulfilmentToProcessRepository fulfilmentToProcessRepository;

  public FulfilmentService(FulfilmentToProcessRepository fulfilmentToProcessRepository) {
    this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
  }

  public void processPrintFulfilment(
      Case caze,
      String packCode,
      UUID correlationId,
      String originatingUser,
      Object metaData,
      ScheduledTask scheduledTask) {
    ExportFileTemplate exportFileTemplate = getAllowedPrintTemplate(packCode, caze);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setExportFileTemplate(exportFileTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setCorrelationId(correlationId);
    fulfilmentToProcess.setOriginatingUser(originatingUser);
    fulfilmentToProcess.setUacMetadata(metaData);
    fulfilmentToProcess.setScheduledTask(scheduledTask);

    fulfilmentToProcessRepository.saveAndFlush(fulfilmentToProcess);
  }

  private ExportFileTemplate getAllowedPrintTemplate(String packCode, Case caze) {
    Survey survey = caze.getCollectionExercise().getSurvey();

    for (FulfilmentSurveyExportFileTemplate fulfilmentSurveyExportFileTemplate :
        survey.getFulfilmentExportFileTemplates()) {
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
            packCode, survey.getName()));
  }
}
