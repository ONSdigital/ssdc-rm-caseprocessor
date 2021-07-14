package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class CaseToProcess {

  @Id
  @Column(columnDefinition = "serial")
  private long id;

  @ManyToOne private Case caze;

  @ManyToOne private ActionRule actionRule;

  @Column private UUID batchId;

  @Column(nullable = false)
  private int batchQuantity;
}
