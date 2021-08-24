package uk.gov.ons.ssdc.caseprocessor.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
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
    EventHeaderDTO eventHeader = EventHelper.createEventDTO("Test topic");
    eventHeader.setSource("Test source");
    eventHeader.setChannel("Test channel");

    UacQidDTO redactMe = new UacQidDTO();
    redactMe.setQid("TEST QID");
    redactMe.setUac("SECRET UAC");

    underTest.logCaseEvent(
        caze,
        eventTime,
        "Test description",
        EventType.NEW_CASE,
        eventHeader,
        redactMe,
        messageTime);

    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    Event actualEvent = eventArgumentCaptor.getValue();
    assertThat(caze).isEqualTo(actualEvent.getCaze());
    assertThat(actualEvent.getUacQidLink()).isNull();
    assertThat(eventTime).isEqualTo(actualEvent.getDateTime());
    assertThat("Test source").isEqualTo(actualEvent.getSource());
    assertThat("Test channel").isEqualTo(actualEvent.getChannel());
    assertThat(EventType.NEW_CASE).isEqualTo(actualEvent.getType());
    assertThat("Test description").isEqualTo(actualEvent.getDescription());
    assertThat("{\"uac\":\"REDACTED\",\"qid\":\"TEST QID\"}").isEqualTo(actualEvent.getPayload());
    assertThat(eventHeader.getMessageId()).isEqualTo(actualEvent.getMessageId());
    assertThat(messageTime).isEqualTo(actualEvent.getMessageTimestamp());
  }

  @Test
  public void testLogUacQidEvent() {
    UacQidLink uacQidLink = new UacQidLink();
    OffsetDateTime eventTime = OffsetDateTime.now();
    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(30);
    EventHeaderDTO eventHeader = EventHelper.createEventDTO("Test topic");
    eventHeader.setSource("Test source");
    eventHeader.setChannel("Test channel");

    UacQidDTO redactMe = new UacQidDTO();
    redactMe.setQid("TEST QID");
    redactMe.setUac("SECRET UAC");

    underTest.logUacQidEvent(
        uacQidLink,
        eventTime,
        "Test description",
        EventType.NEW_CASE,
        eventHeader,
        redactMe,
        messageTime);

    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    Event actualEvent = eventArgumentCaptor.getValue();
    assertThat(uacQidLink).isEqualTo(actualEvent.getUacQidLink());
    assertThat(actualEvent.getCaze()).isNull();
    assertThat(eventTime).isEqualTo(actualEvent.getDateTime());
    assertThat("Test source").isEqualTo(actualEvent.getSource());
    assertThat("Test channel").isEqualTo(actualEvent.getChannel());
    assertThat(EventType.NEW_CASE).isEqualTo(actualEvent.getType());
    assertThat("Test description").isEqualTo(actualEvent.getDescription());
    assertThat("{\"uac\":\"REDACTED\",\"qid\":\"TEST QID\"}").isEqualTo(actualEvent.getPayload());
    assertThat(eventHeader.getMessageId()).isEqualTo(actualEvent.getMessageId());
    assertThat(messageTime).isEqualTo(actualEvent.getMessageTimestamp());
  }
}
