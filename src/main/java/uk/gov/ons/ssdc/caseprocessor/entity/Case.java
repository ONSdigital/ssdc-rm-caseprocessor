package uk.gov.ons.ssdc.caseprocessor.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cases", schema = "casev3")
public class Case {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "case_ref")
    private Long caseRef;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "invalid", nullable = false)
    private Boolean invalid = false;

    @Column(name = "last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @Column(name = "refusal_received")
    private String refusalReceived;

    @Column(name = "sample")
    @Type(type = "com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType")
    private JsonNode sample;

    @Column(name = "sample_sensitive")
    @Type(type = "com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType")
    private JsonNode sampleSensitive;

    @Column(name = "secret_sequence_number", nullable = false)
    private Integer secretSequenceNumber;

}