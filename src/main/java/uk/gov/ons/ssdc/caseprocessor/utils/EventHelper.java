package uk.gov.ons.ssdc.caseprocessor.utils;

import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;

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

    eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
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

  public static EventDTO getDummyEvent(UUID correlationId, String originatingUser) {
    return getDummyEvent(correlationId, originatingUser, null);
  }

  public static EventDTO getDummyEvent(
      UUID correlationId, String originatingUser, PayloadDTO payloadDTO) {
    EventHeaderDTO eventHeader = new EventHeaderDTO();

    eventHeader.setChannel(EVENT_CHANNEL);
    eventHeader.setSource(EVENT_SOURCE);
    eventHeader.setMessageId(UUID.randomUUID());
    eventHeader.setCorrelationId(correlationId);
    eventHeader.setOriginatingUser(originatingUser);
    eventHeader.setDateTime(OffsetDateTime.now());

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    return event;
  }
}
