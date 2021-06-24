package uk.gov.ons.ssdc.caseprocessor.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
public class FulfilmentNextTrigger {
    @Id private UUID id;

    @Column(columnDefinition = "timestamp with time zone")
    private OffsetDateTime triggerDateTime;
}
