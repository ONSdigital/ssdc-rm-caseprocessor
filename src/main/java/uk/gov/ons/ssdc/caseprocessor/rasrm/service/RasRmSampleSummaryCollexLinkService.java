package uk.gov.ons.ssdc.caseprocessor.rasrm.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.rasrm.client.RasRmPartyServiceClient;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;

@Component
public class RasRmSampleSummaryCollexLinkService {
  private static final Set<String> MANDATORY_COLLEX_METADATA =
      Set.of("rasRmSampleSummaryId", "rasRmCollectionExerciseId");

  private final RasRmPartyServiceClient rasRmPartyServiceClient;

  public RasRmSampleSummaryCollexLinkService(RasRmPartyServiceClient rasRmPartyServiceClient) {
    this.rasRmPartyServiceClient = rasRmPartyServiceClient;
  }

  public void linkSampleSummaryToCollex(CollectionExercise collectionExercise) {
    Object metadataObject = collectionExercise.getMetadata();

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

    if (!metadata.keySet().containsAll(MANDATORY_COLLEX_METADATA)) {
      throw new RuntimeException("Metadata does not contain mandatory values");
    }

    UUID rasRmSampleSummaryId = UUID.fromString((String) metadata.get("rasRmSampleSummaryId"));
    UUID rasRmCollectionExerciseId =
        UUID.fromString((String) metadata.get("rasRmCollectionExerciseId"));

    rasRmPartyServiceClient.linkSampleSummaryToCollex(
        rasRmSampleSummaryId, rasRmCollectionExerciseId);
  }
}
