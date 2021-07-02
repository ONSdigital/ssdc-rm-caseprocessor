package uk.gov.ons.ssdc.caseprocessor.model.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@ToString(onlyExplicitlyIncluded = true) // Bidirectional relationship causes IDE stackoverflow
@Data
@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
public class ActionRule {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column
  private ActionRuleType type;

  @Column(columnDefinition = "timestamp with time zone")
  private OffsetDateTime triggerDateTime;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean hasTriggered;

  @Lob
  @Type(type = "org.hibernate.type.BinaryType")
  @Column
  private byte[] classifiers;

  @ManyToOne private PrintTemplate printTemplate;

  @ManyToOne private CollectionExercise collectionExercise;

  public void setClassifiers(String classifierClauseStr) {
    classifiers = classifierClauseStr.getBytes();
  }

  public String getClassifiers() {
    if (classifiers == null) {
      return null;
    }

    return new String(classifiers);
  }
}
