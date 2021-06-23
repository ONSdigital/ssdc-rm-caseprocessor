package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.util.Collection;
import java.util.UUID;
import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
  @Id private UUID id;

  @Column private String email;

  @ElementCollection(targetClass = Survey.class)
  @CollectionTable
  @Enumerated(EnumType.STRING)
  private Collection<Survey> surveys;
}
