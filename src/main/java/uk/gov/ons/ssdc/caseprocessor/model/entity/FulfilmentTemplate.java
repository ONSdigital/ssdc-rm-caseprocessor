package uk.gov.ons.ssdc.caseprocessor.model.entity;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class FulfilmentTemplate {
  @Id private String fulfilmentCode;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private String[] template;

  @Column private String printSupplier;
}
