package uk.gov.ons.ssdc.caseprocessor.utils;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;

public class EventHelper {

  private static final String EVENT_SOURCE = "CASE_PROCESSOR";
  private static final String EVENT_CHANNEL = "RM";

  public static EventDTO createEventDTO(
      EventTypeDTO eventType, String eventChannel, String eventSource) {
    EventDTO eventDTO = new EventDTO();

    eventDTO.setChannel(eventChannel);
    eventDTO.setSource(eventSource);
    eventDTO.setDateTime(OffsetDateTime.now());
    eventDTO.setTransactionId(UUID.randomUUID());
    eventDTO.setType(eventType);

    return eventDTO;
  }

  public static EventDTO createEventDTO(EventTypeDTO eventType) {
    return createEventDTO(eventType, EVENT_CHANNEL, EVENT_SOURCE);
  }

  public static EventDTO getDummyEvent() {
    EventDTO event = new EventDTO();

    event.setChannel(EVENT_CHANNEL);
    event.setSource(EVENT_SOURCE);
    event.setTransactionId(UUID.randomUUID());

    return event;
  }
}
