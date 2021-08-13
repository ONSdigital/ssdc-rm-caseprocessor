package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class EventHeaderDTO {
  private String topic;
  private String source;
  private String channel;
  private OffsetDateTime dateTime;
  private UUID messageId;
  private UUID correlationId;
  private String originatingUser;
}
