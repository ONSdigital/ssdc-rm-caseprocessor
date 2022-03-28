package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentSurveyExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;
import uk.gov.ons.ssdc.common.model.entity.Survey;

@MessageEndpoint
public class PrintFulfilmentReceiver {
  private final CaseService caseService;
  private final EventLogger eventLogger;
  private final FulfilmentToProcessRepository fulfilmentToProcessRepository;
  private static final Logger log = LoggerFactory.getLogger(PrintFulfilmentReceiver.class);

  public PrintFulfilmentReceiver(
      CaseService caseService,
      EventLogger eventLogger,
      FulfilmentToProcessRepository fulfilmentToProcessRepository) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
    this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "printFulfilmentInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    if (fulfilmentToProcessRepository.existsByMessageId(event.getHeader().getMessageId())) {
      log.with("correlationId", event.getHeader().getCorrelationId())
          .with("messageId", event.getHeader().getMessageId())
          .info(
              "Received duplicate fulfilment message ID, ignoring and acking the duplicate message");
      return;
    }

    Case caze = caseService.getCase(event.getPayload().getPrintFulfilment().getCaseId());

    ExportFileTemplate exportFileTemplate =
        getAllowedPrintTemplate(event.getPayload().getPrintFulfilment().getPackCode(), caze);

    FulfilmentToProcess fulfilmentToProcess = new FulfilmentToProcess();
    fulfilmentToProcess.setExportFileTemplate(exportFileTemplate);
    fulfilmentToProcess.setCaze(caze);
    fulfilmentToProcess.setCorrelationId(event.getHeader().getCorrelationId());
    fulfilmentToProcess.setMessageId(event.getHeader().getMessageId());
    fulfilmentToProcess.setOriginatingUser(event.getHeader().getOriginatingUser());
    fulfilmentToProcess.setUacMetadata(event.getPayload().getPrintFulfilment().getUacMetadata());
    fulfilmentToProcess.setPersonalisation(
        event.getPayload().getPrintFulfilment().getPersonalisation());

    fulfilmentToProcessRepository.saveAndFlush(fulfilmentToProcess);

    eventLogger.logCaseEvent(
        caze, "Print fulfilment requested", EventType.PRINT_FULFILMENT, event, message);
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
