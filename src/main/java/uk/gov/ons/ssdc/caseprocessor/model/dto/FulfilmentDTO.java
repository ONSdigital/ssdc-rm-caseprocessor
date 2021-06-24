package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class FulfilmentDTO {
    private UUID caseId;
    private String fulfilmentCode;
}
