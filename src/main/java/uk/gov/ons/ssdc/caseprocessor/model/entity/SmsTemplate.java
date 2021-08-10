package uk.gov.ons.ssdc.caseprocessor.model.entity;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Data
@Entity
public class SmsTemplate {
  @Id private String packCode;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private String[] template;

  @Column private UUID templateId;
}
