package uk.gov.ons.ssdc.caseprocessor.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@MessageEndpoint
public class SurveyLaunchedReceiver {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

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
  public void receiveMessage(Message<byte[]> message) {
    byte[] rawMessageBody = message.getPayload();

    ResponseManagementEvent responseManagementEvent;
    try {
      responseManagementEvent = objectMapper.readValue(rawMessageBody, ResponseManagementEvent.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String logEventDescription;
    EventType logEventType;

    switch (responseManagementEvent.getEvent().getType()) {
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
                "Event Type '%s' is invalid on this topic", responseManagementEvent.getEvent().getType()));
    }

    UacQidLink uacQidLink =
        uacService.findByQid(responseManagementEvent.getPayload().getResponse().getQuestionnaireId());

    eventLogger.logUacQidEvent(
        uacQidLink,
        responseManagementEvent.getEvent().getDateTime(),
        logEventDescription,
        logEventType,
        responseManagementEvent.getEvent(),
        responseManagementEvent.getPayload().getResponse(),
        OffsetDateTime.now());

    if (logEventType == EventType.SURVEY_LAUNCHED && uacQidLink.getCaze() != null) {
      uacQidLink.getCaze().setSurveyLaunched(true);
      caseService.saveCaseAndEmitCaseUpdatedEvent(uacQidLink.getCaze());
    }
  }
}
