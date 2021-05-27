package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Data;
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true) // Bidirectional relationship causes IDE stackoverflow
@Data
@Entity
public class Survey {
  @Id private UUID id;

  @Column private String name;

  @OneToMany(mappedBy = "survey")
  private List<CollectionExercise> collectionExercises;
}
