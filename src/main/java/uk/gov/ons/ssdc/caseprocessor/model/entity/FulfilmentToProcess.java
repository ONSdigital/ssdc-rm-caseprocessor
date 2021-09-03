package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.util.UUID;
import javax.persistence.*;
import lombok.Data;

@Entity
@Data
public class FulfilmentToProcess {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @ManyToOne(optional = false)
  private PrintTemplate printTemplate;

  @ManyToOne(optional = false)
  private Case caze;

  @Column private Integer batchQuantity;

  @Column private UUID batchId;
}
