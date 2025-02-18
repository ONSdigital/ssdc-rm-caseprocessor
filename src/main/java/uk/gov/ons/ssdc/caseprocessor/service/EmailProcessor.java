package uk.gov.ons.ssdc.caseprocessor.service;

import static uk.gov.ons.ssdc.caseprocessor.model.dto.CloudTaskType.NOTIFY_REQUEST;
import static uk.gov.ons.ssdc.caseprocessor.utils.PersonalisationTemplateHelper.buildPersonalisationFromTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.*;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EmailTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.common.model.entity.*;

@Component
public class EmailProcessor {
  private final MessageSender messageSender;
  private final EventLogger eventLogger;
  private final EmailRequestService emailRequestService;
  private final EmailTemplateRepository emailTemplateRepository;
  private final CaseRepository caseRepository;
  private final UacService uacService;

  @Value("${queueconfig.email-request-topic}")
  private String emailRequestTopic;

  @Value("${queueconfig.cloud-task-queue-topic}")
  private String cloudTaskQueueTopic;

  public EmailProcessor(
      MessageSender messageSender,
      EventLogger eventLogger,
      EmailRequestService emailRequestService,
      EmailTemplateRepository emailTemplateRepository,
      CaseRepository caseRepository,
      UacService uacService) {
    this.messageSender = messageSender;
    this.eventLogger = eventLogger;
    this.emailRequestService = emailRequestService;
    this.emailTemplateRepository = emailTemplateRepository;
    this.caseRepository = caseRepository;
    this.uacService = uacService;
  }

  public void process(Case caze, ActionRule actionRule) {
    UUID caseId = caze.getId();
    String packCode = actionRule.getEmailTemplate().getPackCode();
    String email = caze.getSampleSensitive().get(actionRule.getEmailColumn());

    EmailRequest emailRequest = new EmailRequest();
    emailRequest.setCaseId(caseId);
    emailRequest.setPackCode(packCode);
    emailRequest.setEmail(email);
    emailRequest.setUacMetadata(actionRule.getUacMetadata());
    emailRequest.setScheduled(true);

    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO(
            emailRequestTopic, actionRule.getId(), actionRule.getCreatedBy());

    EventDTO event = new EventDTO();
    PayloadDTO payload = new PayloadDTO();
    event.setHeader(eventHeader);
    event.setPayload(payload);
    payload.setEmailRequest(emailRequest);

    messageSender.sendMessage(emailRequestTopic, event);

    eventLogger.logCaseEvent(
        caze,
        String.format("Email requested by action rule for pack code %s", packCode),
        EventType.ACTION_RULE_EMAIL_REQUEST,
        event,
        OffsetDateTime.now());
  }

  public void newProcess(Case caze, ActionRule actionRule) {
    UUID caseId = caze.getId();
    String packCode = actionRule.getEmailTemplate().getPackCode();
    String email = caze.getSampleSensitive().get(actionRule.getEmailColumn());
    validateEmailAddress(email);

    // Get template
    EmailTemplate emailTemplate =
            emailTemplateRepository
                    .findById(packCode)
                    .orElseThrow(
                            () ->
                                    new RuntimeException(
                                            "Email template not found: " + packCode));

    if (!caseRepository.existsById(caseId)) {
      throw new RuntimeException("Case not found with ID: " + caseId);
    }

    Optional<UacQidCreatedPayloadDTO> newUacQidPair =
        emailRequestService.fetchNewUacQidPairIfRequired(emailTemplate.getTemplate());
    if (newUacQidPair.isPresent()) {
      UacQidCreatedPayloadDTO uacQidCreatedPayloadDTO = newUacQidPair.get();
      if (uacQidCreatedPayloadDTO.getQid() != null) {
        // Check the QID does not already exist
        if (uacService.existsByQid(uacQidCreatedPayloadDTO.getQid())) {

          // If it does exist, check if it is linked to the given case
          UacQidLink existingUacQidLink = uacService.findByQid(uacQidCreatedPayloadDTO.getQid());
          if (existingUacQidLink.getCaze().getId().equals(caseId)) {

            // If the QID is already linked to the given case this must be duplicate event, ignore
            return;
          }

          // If not then something has gone wrong, error out
          throw new RuntimeException(
              "Email fulfilment QID "
                  + uacQidCreatedPayloadDTO.getQid()
                  + " is already linked to a different case");
        }
        uacService.createLinkAndEmitNewUacQid(
            caze,
            uacQidCreatedPayloadDTO.getUac(),
            uacQidCreatedPayloadDTO.getQid(),
            actionRule.getUacMetadata(),
            actionRule.getId(),
            actionRule.getCreatedBy());
      }
    }
    // Build personalisation
    // TODO look into the request personalisation here as it shouldn't be null.
    Map<String, String> personalisationTemplateValues =
        buildPersonalisationTemplateValues(emailTemplate.getTemplate(), caze, newUacQidPair, null);

    // Send cloud task request
    CloudTaskMessage cloudTaskMessage =
        prepareNotifyRequestCloudTask(
            emailTemplate.getNotifyTemplateId().toString(),
            emailTemplate.getNotifyServiceRef(),
            email,
            personalisationTemplateValues,
            actionRule.getId().toString(),
            actionRule.getId());
    messageSender.sendMessage(cloudTaskQueueTopic, cloudTaskMessage);
  }

  private void validateEmailAddress(String emailAddress) {
    Optional<String> validationFailure = emailRequestService.validateEmailAddress(emailAddress);
    if (validationFailure.isPresent()) {
      String responseMessage = String.format("Invalid email address: %s", validationFailure.get());
      throw new RuntimeException(responseMessage);
    }
  }

  private CloudTaskMessage prepareNotifyRequestCloudTask(
      String notifyTemplateId,
      String notifyServiceRef,
      String email,
      Map<String, String> personalisation,
      String correlationId,
      UUID transactionId) {
    // Send cloud task request
    CloudTaskMessage cloudTaskMessage = new CloudTaskMessage();
    cloudTaskMessage.setCloudTaskType(NOTIFY_REQUEST);
    cloudTaskMessage.setCorrelationId(transactionId);

    NotifyRequestTaskPayload notifyRequestTaskPayload = new NotifyRequestTaskPayload();
    notifyRequestTaskPayload.setEmail(email);
    notifyRequestTaskPayload.setNotifyTemplateId(notifyTemplateId);
    notifyRequestTaskPayload.setNotifyServiceRef(notifyServiceRef);
    notifyRequestTaskPayload.setTransactionId(transactionId);
    notifyRequestTaskPayload.setPersonalisation(personalisation);
    notifyRequestTaskPayload.setCorrelationId(correlationId);
    cloudTaskMessage.setPayload(notifyRequestTaskPayload);
    return cloudTaskMessage;
  }

  private Map<String, String> buildPersonalisationTemplateValues(
      String[] template,
      Case caze,
      Optional<UacQidCreatedPayloadDTO> uacQidPair,
      Map<String, String> requestPersonalisation) {
    if (uacQidPair.isPresent()) {
      return buildPersonalisationFromTemplate(
          template,
          caze,
          uacQidPair.get().getUac(),
          uacQidPair.get().getQid(),
          requestPersonalisation);
    }
    return buildPersonalisationFromTemplate(template, caze, requestPersonalisation);
  }
}
