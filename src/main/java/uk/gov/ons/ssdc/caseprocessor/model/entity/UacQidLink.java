package uk.gov.ons.ssdc.caseprocessor.model.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@ToString(onlyExplicitlyIncluded = true) // Bidirectional relationship causes IDE stackoverflow
@Data
@Entity
public class UacQidLink {
  @Id private UUID id;

  @Column private String qid;

  @Column private String uac;

  @ManyToOne private Case caze;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
  private boolean active = true;

  @Column(columnDefinition = "timestamp with time zone")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(columnDefinition = "timestamp with time zone")
  @UpdateTimestamp
  private OffsetDateTime lastUpdatedAt;
}
