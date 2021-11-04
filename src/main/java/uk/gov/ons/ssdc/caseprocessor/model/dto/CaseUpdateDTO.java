package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class CaseUpdateDTO {
  private UUID caseId;
  private UUID collectionExerciseId;
  private UUID surveyId;
  private boolean invalid;
  private RefusalTypeDTO refusalReceived;
  private Map<String, String> sample;
  private Map<String, String> sampleSensitive;
}
