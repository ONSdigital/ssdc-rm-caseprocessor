package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.Type;

@Data
@Entity
public class SmsTemplate {
  @Id private String packCode;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private String[] template;

  @Column private UUID templateId;
}
