package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class MessageToSend {
  @Id
  private UUID id;

  @Column
  private String destinationTopic;

  @Column
  private String messageBody;
}
