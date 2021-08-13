package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;

public class EventHelperTest {

  @Test
  public void testCreateEventDTOWithEventType() {
    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.CASE_UPDATE);

    assertThat(eventDTO.getChannel()).isEqualTo("RM");
    assertThat(eventDTO.getSource()).isEqualTo("CASE_PROCESSOR");
    assertThat(eventDTO.getDateTime()).isInstanceOf(OffsetDateTime.class);
    assertThat(eventDTO.getTransactionId()).isInstanceOf(UUID.class);
    assertThat(eventDTO.getType()).isEqualTo(EventTypeDTO.CASE_UPDATE);
  }

  @Test
  public void testCreateEventDTOWithEventTypeChannelAndSource() {
    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.CASE_UPDATE, "CHANNEL", "SOURCE");

    assertThat(eventDTO.getChannel()).isEqualTo("CHANNEL");
    assertThat(eventDTO.getSource()).isEqualTo("SOURCE");
    assertThat(eventDTO.getDateTime()).isInstanceOf(OffsetDateTime.class);
    assertThat(eventDTO.getTransactionId()).isInstanceOf(UUID.class);
    assertThat(eventDTO.getType()).isEqualTo(EventTypeDTO.CASE_UPDATE);
  }

  @Test
  public void testGetDummyEvent() {
    EventDTO eventDTO = EventHelper.getDummyEvent();

    assertThat(eventDTO.getChannel()).isEqualTo("RM");
    assertThat(eventDTO.getSource()).isEqualTo("CASE_PROCESSOR");
    assertThat(eventDTO.getTransactionId()).isInstanceOf(UUID.class);
  }
}
