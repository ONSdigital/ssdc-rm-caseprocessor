package uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class RasRmCaseNotification {
  private UUID id;
  private boolean activeEnrolment;
  private String sampleUnitRef;
  private String sampleUnitType;
  private UUID partyId;
  private UUID collectionInstrumentId;
  private UUID collectionExerciseId;
}
