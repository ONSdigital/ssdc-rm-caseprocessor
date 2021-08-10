package uk.gov.ons.ssdc.caseprocessor.model.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Data
@Entity
public class FulfilmentSurveySmsTemplate {
  @Id private UUID id;

  @ManyToOne private Survey survey;

  @ManyToOne private SmsTemplate smsTemplate;
}
