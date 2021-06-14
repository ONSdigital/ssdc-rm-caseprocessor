package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.util.Collection;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import lombok.Data;

@Entity
@Data
public class User {
  @Id private UUID id;

  @Column private String email;

  @ElementCollection(targetClass = BulkProcess.class)
  @CollectionTable
  @Enumerated(EnumType.STRING)
  private Collection<BulkProcess> bulkProcesses;
}
