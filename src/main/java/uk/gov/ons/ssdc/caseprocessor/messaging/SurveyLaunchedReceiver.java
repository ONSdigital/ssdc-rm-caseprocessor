package uk.gov.ons.ssdc.caseprocessor.messaging;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
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
  @ServiceActivator(inputChannel = "surveyLaunchedInputChannel")
  public void receiveMessage(ResponseManagementEvent surveyEvent) {

    if (!processEvent(surveyEvent)) {
      return;
    }

    UacQidLink surveyLaunchedForQid =
        uacService.findByQid(surveyEvent.getPayload().getResponse().getQuestionnaireId());

    Case caze = surveyLaunchedForQid.getCaze();

    if (caze != null && surveyEvent.getEvent().getChannel().equals("RH")) {
      caze.setSurveyLaunched(true);
      caseService.saveCaseAndEmitCaseUpdatedEvent(caze);
    }

    eventLogger.logUacQidEvent(
        surveyLaunchedForQid,
        surveyEvent.getEvent().getDateTime(),
        "Survey launched",
        EventType.SURVEY_LAUNCHED,
        surveyEvent.getEvent(),
        surveyEvent.getPayload().getResponse(),
        OffsetDateTime.now());
  }

  private boolean processEvent(ResponseManagementEvent surveyEvent) {
    String logEventDescription;
    EventType logEventType;
    ResponseDTO logEventPayload;
    EventDTO event = surveyEvent.getEvent();

    switch (event.getType()) {
      case SURVEY_LAUNCHED:
        return true;

      case RESPONDENT_AUTHENTICATED:
        logEventDescription = "Respondent authenticated";
        logEventType = EventType.RESPONDENT_AUTHENTICATED;
        logEventPayload = surveyEvent.getPayload().getResponse();
        break;

      default:
        // Should never get here
        throw new RuntimeException(
            String.format("Event Type '%s' is invalid on this topic", event.getType()));
    }

    UacQidLink uacQidLink =
        uacService.findByQid(surveyEvent.getPayload().getResponse().getQuestionnaireId());

    eventLogger.logUacQidEvent(
        uacQidLink,
        surveyEvent.getEvent().getDateTime(),
        logEventDescription,
        logEventType,
        surveyEvent.getEvent(),
        logEventPayload,
        OffsetDateTime.now());

    return false;
  }
}
