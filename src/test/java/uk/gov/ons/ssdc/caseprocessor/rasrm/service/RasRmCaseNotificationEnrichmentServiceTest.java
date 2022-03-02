package uk.gov.ons.ssdc.caseprocessor.rasrm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.rasrm.client.RasRmPartyServiceClient;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmCaseNotification;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmPartyAssociationDTO;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmPartyResponseDTO;

@ExtendWith(MockitoExtension.class)
class RasRmCaseNotificationEnrichmentServiceTest {

  @Mock private RasRmPartyServiceClient rasRmPartyServiceClient;
  @Mock private MessageSender messageSender;

  @InjectMocks private RasRmCaseNotificationEnrichmentService underTest;

  @Test
  void notifyRasRmAndEnrichSample() {
    ReflectionTestUtils.setField(underTest, "rasRmCaseNotificationTopic", "Test topic");
    ReflectionTestUtils.setField(underTest, "rasRmPubsubProject", "Test project");
    Map<String, String> sample =
        Map.of(
            "ruref",
            "Test ruref",
            "runame1",
            "Test runame1",
            "froempment",
            "123",
            "frotover",
            "456",
            "cell_no",
            "789");

    UUID rasRmSampleSummaryId = UUID.randomUUID();
    UUID rasRmCollectionExerciseId = UUID.randomUUID();
    UUID rasRmCollectionInstrumentId = UUID.randomUUID();
    Map<String, String> metadata =
        Map.of(
            "rasRmSampleSummaryId",
            rasRmSampleSummaryId.toString(),
            "rasRmCollectionExerciseId",
            rasRmCollectionExerciseId.toString(),
            "rasRmCollectionInstrumentId",
            rasRmCollectionInstrumentId.toString());

    RasRmPartyAssociationDTO rasRmPartyAssociation = new RasRmPartyAssociationDTO();
    rasRmPartyAssociation.setBusinessRespondentStatus("ACTIVE");
    RasRmPartyAssociationDTO[] rasRmPartyAssociations =
        new RasRmPartyAssociationDTO[] {rasRmPartyAssociation};

    UUID partyId = UUID.randomUUID();
    RasRmPartyResponseDTO party = new RasRmPartyResponseDTO();
    party.setAssociations(rasRmPartyAssociations);
    party.setId(partyId);

    when(rasRmPartyServiceClient.createParty(anyString(), any(UUID.class), any(Map.class)))
        .thenReturn(party);

    underTest.notifyRasRmAndEnrichSample(sample, metadata);

    ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(rasRmPartyServiceClient)
        .createParty(eq("Test ruref"), eq(rasRmSampleSummaryId), attributesCaptor.capture());
    Map<String, Object> actualAttributes = attributesCaptor.getValue();
    assertThat(actualAttributes.get("ruref")).isEqualTo("Test ruref");
    assertThat(actualAttributes.get("runame1")).isEqualTo("Test runame1");
    assertThat(actualAttributes.get("froempment")).isEqualTo(123);
    assertThat(actualAttributes.get("frotover")).isEqualTo(456);
    assertThat(actualAttributes.get("cell_no")).isEqualTo(789);

    ArgumentCaptor<RasRmCaseNotification> rasRmCaseNotificationArgCaptor =
        ArgumentCaptor.forClass(RasRmCaseNotification.class);
    verify(messageSender)
        .sendMessage(
            eq("projects/Test project/topics/Test topic"),
            rasRmCaseNotificationArgCaptor.capture());
    RasRmCaseNotification actualRasRmCaseNotification = rasRmCaseNotificationArgCaptor.getValue();
    assertThat(actualRasRmCaseNotification.getCollectionExerciseId())
        .isEqualTo(rasRmCollectionExerciseId);
    assertThat(actualRasRmCaseNotification.getCollectionInstrumentId())
        .isEqualTo(rasRmCollectionInstrumentId);
    assertThat(actualRasRmCaseNotification.getSampleUnitRef()).isEqualTo("Test ruref");
    assertThat(actualRasRmCaseNotification.getPartyId()).isEqualTo(partyId);
    assertThat(actualRasRmCaseNotification.getSampleUnitType()).isEqualTo("B");
    assertThat(actualRasRmCaseNotification.isActiveEnrolment()).isTrue();
  }

  @Test
  public void testNullMetaDataException() {
    RuntimeException thrown =
            assertThrows(RuntimeException.class, () -> underTest.notifyRasRmAndEnrichSample(null, null));
    assertThat(thrown.getMessage())
            .isEqualTo("Unexpected null metadata. Metadata is required for RAS-RM business.");
  }

  @Test
  public void testMetaDataNotAMap() {
    RuntimeException thrown =
            assertThrows(RuntimeException.class, () -> underTest.notifyRasRmAndEnrichSample(null, "NotAMapButAString"));
    assertThat(thrown.getMessage())
            .isEqualTo("Unexpected metadata type. Wanted Map but got String");
  }

  @Test
  public void testMissingMANDATORY_COLLEX_METADATA() {
    RuntimeException thrown =
            assertThrows(RuntimeException.class, () -> underTest.notifyRasRmAndEnrichSample(null,
                    Map.of("Not", "TheRequiredFields")));
    assertThat(thrown.getMessage())
            .isEqualTo("Metadata does not contain mandatory values");
  }

  @Test
  public void testSampleMissingRequiredColumn() {
    ReflectionTestUtils.setField(underTest, "rasRmCaseNotificationTopic", "Test topic");
    ReflectionTestUtils.setField(underTest, "rasRmPubsubProject", "Test project");
    Map<String, String> sample =
            Map.of(
                    "rurefWrong",
                    "Test ruref",
                    "runame1",
                    "Test runame1",
                    "froempment",
                    "123",
                    "frotover",
                    "456",
                    "cell_no",
                    "789");

    UUID rasRmSampleSummaryId = UUID.randomUUID();
    UUID rasRmCollectionExerciseId = UUID.randomUUID();
    UUID rasRmCollectionInstrumentId = UUID.randomUUID();
    Map<String, String> metadata =
            Map.of(
                    "rasRmSampleSummaryId",
                    rasRmSampleSummaryId.toString(),
                    "rasRmCollectionExerciseId",
                    rasRmCollectionExerciseId.toString(),
                    "rasRmCollectionInstrumentId",
                    rasRmCollectionInstrumentId.toString());

    RuntimeException thrown =
            assertThrows(RuntimeException.class, () -> underTest.notifyRasRmAndEnrichSample(sample, metadata));
    assertThat(thrown.getMessage())
            .isEqualTo("Cannot notify RAS-RM of business case which does not have column: ruref");
  }
}
