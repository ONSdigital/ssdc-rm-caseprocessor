package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;

public class EventHelperTest {

  @Test
  public void testCreateEventDTOWithEventType() {
    EventHeaderDTO eventHeader = EventHelper.createEventDTO("TOPIC");

    assertThat(eventHeader.getVersion()).isEqualTo(EVENT_SCHEMA_VERSION);
    assertThat(eventHeader.getTopic()).isEqualTo("TOPIC");
    assertThat(eventHeader.getChannel()).isEqualTo("RM");
    assertThat(eventHeader.getSource()).isEqualTo("CASE_PROCESSOR");
    assertThat(eventHeader.getDateTime()).isInstanceOf(OffsetDateTime.class);
    assertThat(eventHeader.getMessageId()).isInstanceOf(UUID.class);
  }

  @Test
  public void testCreateEventDTOWithEventTypeChannelAndSource() {
    EventHeaderDTO eventHeader = EventHelper.createEventDTO("TOPIC", "CHANNEL", "SOURCE");

    assertThat(eventHeader.getVersion()).isEqualTo(EVENT_SCHEMA_VERSION);
    assertThat(eventHeader.getChannel()).isEqualTo("CHANNEL");
    assertThat(eventHeader.getSource()).isEqualTo("SOURCE");
    assertThat(eventHeader.getDateTime()).isInstanceOf(OffsetDateTime.class);
    assertThat(eventHeader.getMessageId()).isInstanceOf(UUID.class);
    assertThat(eventHeader.getTopic()).isEqualTo("TOPIC");
  }

  @Test
  public void testGetDummyEvent() {
    EventHeaderDTO eventHeader = EventHelper.getDummyEvent();

    assertThat(eventHeader.getChannel()).isEqualTo("RM");
    assertThat(eventHeader.getSource()).isEqualTo("CASE_PROCESSOR");
    assertThat(eventHeader.getMessageId()).isInstanceOf(UUID.class);
  }
}
