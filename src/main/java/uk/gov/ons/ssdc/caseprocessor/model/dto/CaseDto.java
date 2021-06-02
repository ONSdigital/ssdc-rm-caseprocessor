package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class CaseDto {
    private UUID caseId;
    private Map<String, String> caze;
}
