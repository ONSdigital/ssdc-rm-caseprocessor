package uk.gov.ons.ssdc.caseprocessor.logging;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.RedactHelper;

@Component
public class EventLogger {

  private final EventRepository eventRepository;

  public EventLogger(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public void logCaseEvent(
      Case caze,
      OffsetDateTime eventDate,
      String eventDescription,
      EventType eventType,
      EventHeaderDTO event,
      Object eventPayload,
      OffsetDateTime messageTimestamp) {
    Event loggedEvent =
        buildEvent(
            eventDate,
            eventDescription,
            eventType,
            event,
            RedactHelper.redact(eventPayload),
            messageTimestamp);
    loggedEvent.setCaze(caze);

    eventRepository.save(loggedEvent);
  }

  public void logUacQidEvent(
      UacQidLink uacQidLink,
      OffsetDateTime eventDate,
      String eventDescription,
      EventType eventType,
      EventHeaderDTO event,
      Object eventPayload,
      OffsetDateTime messageTimestamp) {
    Event loggedEvent =
        buildEvent(
            eventDate,
            eventDescription,
            eventType,
            event,
            RedactHelper.redact(eventPayload),
            messageTimestamp);
    loggedEvent.setUacQidLink(uacQidLink);

    eventRepository.save(loggedEvent);
  }

  private Event buildEvent(
      OffsetDateTime eventDate,
      String eventDescription,
      EventType eventType,
      EventHeaderDTO event,
      Object eventPayload,
      OffsetDateTime messageTimestamp) {
    Event loggedEvent = new Event();

    loggedEvent.setId(UUID.randomUUID());
    loggedEvent.setDateTime(eventDate);
    loggedEvent.setProcessedAt(OffsetDateTime.now());
    loggedEvent.setDescription(eventDescription);
    loggedEvent.setType(eventType);
    loggedEvent.setChannel(event.getChannel());
    loggedEvent.setSource(event.getSource());
    loggedEvent.setMessageId(event.getMessageId());
    loggedEvent.setMessageTimestamp(messageTimestamp);
    loggedEvent.setCreatedBy(event.getOriginatingUser());
    loggedEvent.setCorrelationId(event.getCorrelationId());

    loggedEvent.setPayload(JsonHelper.convertObjectToJson(eventPayload));

    return loggedEvent;
  }
}
