package uk.gov.ons.ssdc.caseprocessor.logging;

import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.RedactHelper;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@Component
public class EventLogger {

  private final EventRepository eventRepository;

  public EventLogger(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public void logCaseEvent(
      Case caze,
      String eventDescription,
      EventType eventType,
      EventDTO event,
      OffsetDateTime messageTimestamp) {

    EventHeaderDTO eventHeader = event.getHeader();
    OffsetDateTime eventDate = eventHeader.getDateTime();
    Object eventPayload = event.getPayload();

    Event loggedEvent =
        buildEvent(
            eventDate,
            eventDescription,
            eventType,
            eventHeader,
            RedactHelper.redact(eventPayload),
            messageTimestamp);
    loggedEvent.setCaze(caze);

    eventRepository.save(loggedEvent);
  }

  public void logCaseEvent(
      Case caze,
      String eventDescription,
      EventType eventType,
      EventDTO event,
      Message<byte[]> message) {

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    logCaseEvent(caze, eventDescription, eventType, event, messageTimestamp);
  }

  public void logUacQidEvent(
      UacQidLink uacQidLink,
      String eventDescription,
      EventType eventType,
      EventDTO event,
      Message<byte[]> message) {

    EventHeaderDTO eventHeader = event.getHeader();
    OffsetDateTime eventDate = eventHeader.getDateTime();
    Object eventPayload = event.getPayload();
    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    Event loggedEvent =
        buildEvent(
            eventDate,
            eventDescription,
            eventType,
            eventHeader,
            RedactHelper.redact(eventPayload),
            messageTimestamp);
    loggedEvent.setUacQidLink(uacQidLink);

    eventRepository.save(loggedEvent);
  }

  private Event buildEvent(
      OffsetDateTime eventDate,
      String eventDescription,
      EventType eventType,
      EventHeaderDTO eventHeader,
      Object eventPayload,
      OffsetDateTime messageTimestamp) {
    Event loggedEvent = new Event();

    loggedEvent.setId(UUID.randomUUID());
    loggedEvent.setDateTime(eventDate);
    loggedEvent.setProcessedAt(OffsetDateTime.now());
    loggedEvent.setDescription(eventDescription);
    loggedEvent.setType(eventType);
    loggedEvent.setChannel(eventHeader.getChannel());
    loggedEvent.setSource(eventHeader.getSource());
    loggedEvent.setMessageId(eventHeader.getMessageId());
    loggedEvent.setMessageTimestamp(messageTimestamp);
    loggedEvent.setCreatedBy(eventHeader.getOriginatingUser());
    loggedEvent.setCorrelationId(eventHeader.getCorrelationId());

    loggedEvent.setPayload(JsonHelper.convertObjectToJson(eventPayload));

    return loggedEvent;
  }
}
