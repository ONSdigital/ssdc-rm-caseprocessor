package uk.gov.ons.ssdc.caseprocessor.rasrm.service;

import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.rasrm.client.RasRmPartyServiceClient;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;

@ExtendWith(MockitoExtension.class)
class RasRmSampleSummaryCollexLinkServiceTest {
  @Mock private RasRmPartyServiceClient rasRmPartyServiceClient;

  @InjectMocks private RasRmSampleSummaryCollexLinkService underTest;

  @Test
  void linkSampleSummaryToCollex() {
    CollectionExercise collectionExercise = new CollectionExercise();
    UUID rasRmSampleSummaryId = UUID.randomUUID();
    UUID rasRmCollectionExerciseId = UUID.randomUUID();
    collectionExercise.setMetadata(
        Map.of(
            "rasRmSampleSummaryId",
            rasRmSampleSummaryId.toString(),
            "rasRmCollectionExerciseId",
            rasRmCollectionExerciseId.toString()));

    underTest.linkSampleSummaryToCollex(collectionExercise);

    verify(rasRmPartyServiceClient)
        .linkSampleSummaryToCollex(rasRmSampleSummaryId, rasRmCollectionExerciseId);
  }
}
