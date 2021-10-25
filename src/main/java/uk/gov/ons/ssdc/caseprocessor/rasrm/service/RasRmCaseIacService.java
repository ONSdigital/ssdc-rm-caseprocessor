package uk.gov.ons.ssdc.caseprocessor.rasrm.service;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.rasrm.client.RasRmCaseServiceClient;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmCaseIacResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmCaseResponseDTO;
import uk.gov.ons.ssdc.common.model.entity.Case;

@Component
public class RasRmCaseIacService {
  private final RasRmCaseServiceClient rasRmCaseServiceClient;

  public RasRmCaseIacService(RasRmCaseServiceClient rasRmCaseServiceClient) {
    this.rasRmCaseServiceClient = rasRmCaseServiceClient;
  }

  public String getRasRmIac(Case caze) {
    Object metadataObject = caze.getCollectionExercise().getMetadata();

    if (metadataObject == null) {
      throw new RuntimeException(
          "Unexpected null metadata. Metadata is required for RAS-RM business.");
    }

    if (!(metadataObject instanceof Map)) {
      throw new RuntimeException(
          "Unexpected metadata type. Wanted Map but got "
              + metadataObject.getClass().getSimpleName());
    }

    Map metadata = (Map) metadataObject;

    if (!metadata.keySet().contains("rasRmCollectionExerciseId")) {
      throw new RuntimeException("Metadata does not contain mandatory rasRmCollectionExerciseId");
    }

    UUID rasRmCollectionExerciseId =
        UUID.fromString((String) metadata.get("rasRmCollectionExerciseId"));

    UUID partyId = UUID.fromString(caze.getSample().get("partyId"));
    RasRmCaseResponseDTO[] cases = rasRmCaseServiceClient.getCases(partyId);

    RasRmCaseResponseDTO rasRmCaseResponse =
        Arrays.stream(cases)
            .filter(
                rasRmCase ->
                    rasRmCase
                        .getCaseGroup()
                        .getCollectionExerciseId()
                        .equals(rasRmCollectionExerciseId))
            .findAny()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Case does not belong to our collection exercise in RAS RM"));

    UUID rasRmCaseId = rasRmCaseResponse.getId();

    RasRmCaseIacResponseDTO[] rasRmIacs = rasRmCaseServiceClient.getIacs(rasRmCaseId);

    if (rasRmIacs.length == 0) {
      throw new RuntimeException("RAS RM has not made any IAC available for our case");
    }

    return rasRmIacs[0].getIac();
  }
}
