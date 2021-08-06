package uk.gov.ons.ssdc.caseprocessor.messaging;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@MessageEndpoint
public class SurveyLaunchedReceiver {

  private final UacService uacService;
  private final CaseService caseService;
  private final EventLogger eventLogger;

  public SurveyLaunchedReceiver(
      UacService uacService, CaseService caseService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "surveyLaunchedInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(ResponseManagementEvent surveyEvent) {
    String logEventDescription;
    EventType logEventType;

    switch (surveyEvent.getEvent().getType()) {
      case SURVEY_LAUNCHED:
        logEventDescription = "Survey launched";
        logEventType = EventType.SURVEY_LAUNCHED;
        break;

      case RESPONDENT_AUTHENTICATED:
        logEventDescription = "Respondent authenticated";
        logEventType = EventType.RESPONDENT_AUTHENTICATED;
        break;

      default:
        // Should never get here
        throw new RuntimeException(
            String.format(
                "Event Type '%s' is invalid on this topic", surveyEvent.getEvent().getType()));
    }

    UacQidLink uacQidLink =
        uacService.findByQid(surveyEvent.getPayload().getResponse().getQuestionnaireId());

    eventLogger.logUacQidEvent(
        uacQidLink,
        surveyEvent.getEvent().getDateTime(),
        logEventDescription,
        logEventType,
        surveyEvent.getEvent(),
        surveyEvent.getPayload().getResponse(),
        OffsetDateTime.now());

    if (logEventType == EventType.SURVEY_LAUNCHED && uacQidLink.getCaze() != null) {
      uacQidLink.getCaze().setSurveyLaunched(true);
      caseService.saveCaseAndEmitCaseUpdatedEvent(uacQidLink.getCaze());
    }
  }
}
