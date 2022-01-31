package uk.gov.ons.ssdc.caseprocessor.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.SmsRequest;
import uk.gov.ons.ssdc.caseprocessor.scheduled.tasks.ScheduledTaskService;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

@Component
public class SmsProcessor {
  private final MessageSender messageSender;
  private final EventLogger eventLogger;
  private final ScheduledTaskService scheduledTaskService;

  @Value("${queueconfig.sms-request-topic}")
  private String smsRequestTopic;

  public SmsProcessor(
      MessageSender messageSender,
      EventLogger eventLogger,
      ScheduledTaskService scheduledTaskService) {
    this.messageSender = messageSender;
    this.eventLogger = eventLogger;
    this.scheduledTaskService = scheduledTaskService;
  }

  public void processScheduledTask(ScheduledTask scheduledTask) {
    Case caze = scheduledTask.getResponsePeriod().getCaze();
    UUID caseId = caze.getId();
    String packCode = scheduledTask.getScheduledTaskDetails().get("packCode");
    // Where we store this well be one of the fun things.
    // This is a short term 'hack'.  Would be a Caze preferences Column perhaps -
    // This would decide how they liked to be contacted and details?
    //
    String phoneNumber = caze.getSampleSensitive().get("phoneNumber");

    SmsRequest smsRequest = new SmsRequest();
    smsRequest.setCaseId(caseId);
    smsRequest.setPackCode(packCode);
    smsRequest.setPhoneNumber(phoneNumber);
    smsRequest.setUacMetadata(null);
    smsRequest.setScheduled(true);

    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO(smsRequestTopic, scheduledTask.getId(), "SCHEDULED_TASK");

    EventDTO event = new EventDTO();
    PayloadDTO payload = new PayloadDTO();
    event.setHeader(eventHeader);
    event.setPayload(payload);
    payload.setSmsRequest(smsRequest);

    messageSender.sendMessage(smsRequestTopic, event);

    Event loggedEvent =
        eventLogger.logCaseEvent(
            caze,
            String.format("SMS requested by action rule for pack code %s", packCode),
            EventType.ACTION_RULE_SMS_REQUEST,
            event,
            OffsetDateTime.now());

    // Still confused about the SMS creation loop regarding UAC etc.
    scheduledTaskService.updateScheculedTaskSentEvent(scheduledTask, loggedEvent, null);
  }

  public void process(Case caze, ActionRule actionRule) {
    UUID caseId = caze.getId();
    String packCode = actionRule.getSmsTemplate().getPackCode();
    String phoneNumber = caze.getSampleSensitive().get(actionRule.getPhoneNumberColumn());

    SmsRequest smsRequest = new SmsRequest();
    smsRequest.setCaseId(caseId);
    smsRequest.setPackCode(packCode);
    smsRequest.setPhoneNumber(phoneNumber);
    smsRequest.setUacMetadata(actionRule.getUacMetadata());
    smsRequest.setScheduled(true);

    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO(smsRequestTopic, actionRule.getId(), actionRule.getCreatedBy());

    EventDTO event = new EventDTO();
    PayloadDTO payload = new PayloadDTO();
    event.setHeader(eventHeader);
    event.setPayload(payload);
    payload.setSmsRequest(smsRequest);

    messageSender.sendMessage(smsRequestTopic, event);

    eventLogger.logCaseEvent(
        caze,
        String.format("SMS requested by action rule for pack code %s", packCode),
        EventType.ACTION_RULE_SMS_REQUEST,
        event,
        OffsetDateTime.now());
  }
}
