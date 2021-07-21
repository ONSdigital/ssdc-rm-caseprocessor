package uk.gov.ons.ssdc.caseprocessor.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;

@ExtendWith(MockitoExtension.class)
public class EventLoggerTest {
  @Mock EventRepository eventRepository;

  @InjectMocks EventLogger underTest;

  @Test
  public void testLogCaseEvent() {
    Case caze = new Case();
    OffsetDateTime eventTime = OffsetDateTime.now();
    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(30);
    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.CASE_CREATED);
    eventDTO.setSource("Test source");
    eventDTO.setChannel("Test channel");

    UacQidDTO redactMe = new UacQidDTO();
    redactMe.setQid("TEST QID");
    redactMe.setUac("SECRET UAC");

    underTest.logCaseEvent(
        caze,
        eventTime,
        "Test description",
        EventType.CASE_CREATED,
        eventDTO,
        redactMe,
        messageTime);

    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    Event actualEvent = eventArgumentCaptor.getValue();
    assertThat(caze).isEqualTo(actualEvent.getCaze());
    assertThat(actualEvent.getUacQidLink()).isNull();
    assertThat(eventTime).isEqualTo(actualEvent.getEventDate());
    assertThat("Test source").isEqualTo(actualEvent.getEventSource());
    assertThat("Test channel").isEqualTo(actualEvent.getEventChannel());
    assertThat(EventType.CASE_CREATED).isEqualTo(actualEvent.getEventType());
    assertThat("Test description").isEqualTo(actualEvent.getEventDescription());
    assertThat("{\"uac\":\"SECRET UAC\",\"qid\":\"TEST QID\"}")
        .isEqualTo(actualEvent.getEventPayload());
    assertThat(eventDTO.getTransactionId()).isEqualTo(actualEvent.getEventTransactionId());
    assertThat(messageTime).isEqualTo(actualEvent.getMessageTimestamp());
  }

  @Test
  public void testLogUacQidEvent() {
    UacQidLink uacQidLink = new UacQidLink();
    OffsetDateTime eventTime = OffsetDateTime.now();
    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(30);
    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.CASE_CREATED);
    eventDTO.setSource("Test source");
    eventDTO.setChannel("Test channel");

    UacQidDTO redactMe = new UacQidDTO();
    redactMe.setQid("TEST QID");
    redactMe.setUac("SECRET UAC");

    underTest.logUacQidEvent(
        uacQidLink,
        eventTime,
        "Test description",
        EventType.CASE_CREATED,
        eventDTO,
        redactMe,
        messageTime);

    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    Event actualEvent = eventArgumentCaptor.getValue();
    assertThat(uacQidLink).isEqualTo(actualEvent.getUacQidLink());
    assertThat(actualEvent.getCaze()).isNull();
    assertThat(eventTime).isEqualTo(actualEvent.getEventDate());
    assertThat("Test source").isEqualTo(actualEvent.getEventSource());
    assertThat("Test channel").isEqualTo(actualEvent.getEventChannel());
    assertThat(EventType.CASE_CREATED).isEqualTo(actualEvent.getEventType());
    assertThat("Test description").isEqualTo(actualEvent.getEventDescription());
    assertThat("{\"uac\":\"SECRET UAC\",\"qid\":\"TEST QID\"}")
        .isEqualTo(actualEvent.getEventPayload());
    assertThat(eventDTO.getTransactionId()).isEqualTo(actualEvent.getEventTransactionId());
    assertThat(messageTime).isEqualTo(actualEvent.getMessageTimestamp());
  }
}
