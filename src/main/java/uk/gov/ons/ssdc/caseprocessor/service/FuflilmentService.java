package uk.gov.ons.ssdc.caseprocessor.service;

import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.common.model.entity.Survey;


@Service
public class FuflilmentService {
    private final FulfilmentToProcessRepository fulfilmentToProcessRepository;

    public FuflilmentService(
            FulfilmentToProcessRepository fulfilmentToProcessRepository) {
        this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
    }

  public void processPrintFulfilment(Case caze, EventDTO eventDto) {
    ExportFileTemplate exportFileTemplate =
        getAllowedPrintTemplate(eventDto.getPayload().getPrintFulfilment().getPackCode(), caze);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setExportFileTemplate(exportFileTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setCorrelationId(eventDto.getHeader().getCorrelationId());
    fulfilmentToProcess.setOriginatingUser(eventDto.getHeader().getOriginatingUser());
    fulfilmentToProcess.setUacMetadata(eventDto.getPayload().getPrintFulfilment().getUacMetadata());

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
