package uk.gov.ons.ssdc.caseprocessor.utils;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;

public class EventHelper {

  private static final String EVENT_SOURCE = "CASE_SERVICE";
  private static final String EVENT_CHANNEL = "RM";

  public static EventDTO createEventDTO(
      EventTypeDTO eventType, String event_channel, String event_source) {
    EventDTO eventDTO = new EventDTO();

    eventDTO.setChannel(event_channel);
    eventDTO.setSource(event_source);
    eventDTO.setDateTime(OffsetDateTime.now());
    eventDTO.setTransactionId(UUID.randomUUID());
    eventDTO.setType(eventType);

    return eventDTO;
  }

  public static EventDTO createEventDTO(EventTypeDTO eventType) {
    return createEventDTO(eventType, EVENT_CHANNEL, EVENT_SOURCE);
  }
}