package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.PrintTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Survey;
import uk.gov.ons.ssdc.caseprocessor.model.entity.SurveyPrintTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;

@MessageEndpoint
public class FulfilmentReceiver {

  private final CaseService caseService;
  private final EventLogger eventLogger;
  private final FulfilmentToProcessRepository fulfilmentToProcessRepository;

  public FulfilmentReceiver(
      CaseService caseService,
      EventLogger eventLogger,
      FulfilmentToProcessRepository fulfilmentToProcessRepository) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
    this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "fulfilmentInputChannel")
  public void receiveMessage(Message<ResponseManagementEvent> message) {
    ResponseManagementEvent responseManagementEvent = message.getPayload();

    Case caze =
        caseService.getCaseByCaseId(
            responseManagementEvent.getPayload().getFulfilment().getCaseId());

    PrintTemplate printTemplate =
        getAllowedPrintTemplate(
            responseManagementEvent.getPayload().getFulfilment().getPackCode(), caze);

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setPrintTemplate(printTemplate);
    fulfilmentToProcess.setCaze(caze);

    fulfilmentToProcessRepository.saveAndFlush(fulfilmentToProcess);

    eventLogger.logCaseEvent(
        caze,
        responseManagementEvent.getEvent().getDateTime(),
        "Fulfilment requested",
        EventType.FULFILMENT,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload(),
        messageTimestamp);
  }

  private PrintTemplate getAllowedPrintTemplate(String packCode, Case caze) {
    Survey survey = caze.getCollectionExercise().getSurvey();

    for (SurveyPrintTemplate surveyPrintTemplate : survey.getFulfilmentPrintTemplates()) {
      if (surveyPrintTemplate.getPrintTemplate().getPackCode().equals(packCode)) {
        return surveyPrintTemplate.getPrintTemplate();
      }
    }

    throw new RuntimeException(
        String.format(
            "Pack code %s is not allowed as a fulfilment on survey %s",
            packCode, survey.getName()));
  }
}
