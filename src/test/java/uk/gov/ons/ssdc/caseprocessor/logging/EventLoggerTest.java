package uk.gov.ons.ssdc.caseprocessor.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;

import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.NewCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ExtendWith(MockitoExtension.class)
public class EventLoggerTest {
  @Mock EventRepository eventRepository;

  @InjectMocks EventLogger underTest;

  @Test
  public void testLogCaseEventSuppliedDateTime() {
    Case caze = new Case();
    OffsetDateTime eventTime = OffsetDateTime.now();
    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(30);
    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO("Test topic", TEST_CORRELATION_ID, TEST_ORIGINATING_USER);
    eventHeader.setSource("Test source");
    eventHeader.setChannel("Test channel");
    eventHeader.setDateTime(eventTime);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);

    NewCase redactMe = new NewCase();
    redactMe.setSampleSensitive(Map.of("redactThis", "ABC123"));

    PayloadDTO payload = new PayloadDTO();
    payload.setNewCase(redactMe);
    event.setPayload(payload);

    underTest.logCaseEvent(caze, "Test description", EventType.NEW_CASE, event, messageTime, null);

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
    assertThat(actualEvent.getPayload()).contains("REDACTED");
    assertThat(eventHeader.getMessageId()).isEqualTo(actualEvent.getMessageId());
    assertThat(eventHeader.getCorrelationId()).isEqualTo(actualEvent.getCorrelationId());
    assertThat(eventHeader.getOriginatingUser()).isEqualTo(actualEvent.getCreatedBy());
    assertThat(messageTime).isEqualTo(actualEvent.getMessageTimestamp());
  }

  @Test
  public void testLogCaseEventDateTimeFromMessage() {
    Case caze = new Case();
    OffsetDateTime eventTime = OffsetDateTime.now();
    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO("Test topic", TEST_CORRELATION_ID, TEST_ORIGINATING_USER);
    eventHeader.setSource("Test source");
    eventHeader.setChannel("Test channel");
    eventHeader.setDateTime(eventTime);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);

    NewCase redactMe = new NewCase();
    redactMe.setSampleSensitive(Map.of("redactThis", "ABC123"));

    PayloadDTO payload = new PayloadDTO();
    payload.setNewCase(redactMe);
    event.setPayload(payload);

    Message<byte[]> message = mock(Message.class);

    // Now the timestamp fun.  Get one an hour+ ago, to 'prove' no fluking
    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(3911);
    long timeStamp = messageTime.toInstant().toEpochMilli();

    MessageHeaders messageHeaders = mock(MessageHeaders.class);
    when(message.getHeaders()).thenReturn(messageHeaders);

    when(messageHeaders.getTimestamp()).thenReturn(timeStamp);

    underTest.logCaseEvent(caze, "Test description", EventType.NEW_CASE, event, message);

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
    assertThat(actualEvent.getPayload()).contains("REDACTED");
    assertThat(eventHeader.getMessageId()).isEqualTo(actualEvent.getMessageId());
    assertThat(eventHeader.getCorrelationId()).isEqualTo(actualEvent.getCorrelationId());
    assertThat(eventHeader.getOriginatingUser()).isEqualTo(actualEvent.getCreatedBy());
    assertThat(timeStamp).isEqualTo(actualEvent.getMessageTimestamp().toInstant().toEpochMilli());
  }

  @Test
  public void testLogUacQidEvent() {
    UacQidLink uacQidLink = new UacQidLink();
    OffsetDateTime eventTime = OffsetDateTime.now();
    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO("Test topic", TEST_CORRELATION_ID, TEST_ORIGINATING_USER);
    eventHeader.setSource("Test source");
    eventHeader.setChannel("Test channel");
    eventHeader.setDateTime(eventTime);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);

    NewCase redactMe = new NewCase();
    redactMe.setSampleSensitive(Map.of("redactThis", "ABC123"));

    PayloadDTO payload = new PayloadDTO();
    payload.setNewCase(redactMe);
    event.setPayload(payload);

    Message<byte[]> message = mock(Message.class);

    // Now the timestamp fun.  Get one an hour+ ago, to 'prove' no fluking
    OffsetDateTime messageTime = OffsetDateTime.now().minusSeconds(3911);
    long timeStamp = messageTime.toInstant().toEpochMilli();

    MessageHeaders messageHeaders = mock(MessageHeaders.class);
    when(message.getHeaders()).thenReturn(messageHeaders);

    when(messageHeaders.getTimestamp()).thenReturn(timeStamp);

    underTest.logUacQidEvent(uacQidLink, "Test description", EventType.NEW_CASE, event, message);

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
    assertThat(actualEvent.getPayload())
        .contains("\"sampleSensitive\":{\"redactThis\":\"REDACTED\"}");
    assertThat(eventHeader.getMessageId()).isEqualTo(actualEvent.getMessageId());
    assertThat(eventHeader.getCorrelationId()).isEqualTo(actualEvent.getCorrelationId());
    assertThat(eventHeader.getOriginatingUser()).isEqualTo(actualEvent.getCreatedBy());
    assertThat(timeStamp).isEqualTo(actualEvent.getMessageTimestamp().toInstant().toEpochMilli());
  }
}
