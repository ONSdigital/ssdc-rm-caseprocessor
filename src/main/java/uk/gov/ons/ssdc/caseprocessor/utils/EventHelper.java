package uk.gov.ons.ssdc.caseprocessor.utils;

import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;

public class EventHelper {

  private static final String EVENT_SOURCE = "CASE_PROCESSOR";
  private static final String EVENT_CHANNEL = "RM";

  public static EventHeaderDTO createEventDTO(
      String topic,
      String eventChannel,
      String eventSource,
      UUID correlationId,
      String originatingUser) {
    EventHeaderDTO eventHeader = new EventHeaderDTO();

    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    eventHeader.setChannel(eventChannel);
    eventHeader.setSource(eventSource);
    eventHeader.setDateTime(OffsetDateTime.now());
    eventHeader.setMessageId(UUID.randomUUID());
    eventHeader.setCorrelationId(correlationId);
    eventHeader.setOriginatingUser(originatingUser);
    eventHeader.setTopic(topic);

    return eventHeader;
  }

  public static EventHeaderDTO createEventDTO(
      String topic, UUID correlationId, String originatingUser) {
    return createEventDTO(topic, EVENT_CHANNEL, EVENT_SOURCE, correlationId, originatingUser);
  }

  public static EventHeaderDTO getDummyEvent(UUID correlationId, String originatingUser) {
    EventHeaderDTO event = new EventHeaderDTO();

    event.setChannel(EVENT_CHANNEL);
    event.setSource(EVENT_SOURCE);
    event.setMessageId(UUID.randomUUID());
    event.setCorrelationId(correlationId);
    event.setOriginatingUser(originatingUser);

    return event;
  }
}
