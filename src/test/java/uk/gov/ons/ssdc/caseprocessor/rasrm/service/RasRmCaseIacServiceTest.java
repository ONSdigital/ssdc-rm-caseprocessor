package uk.gov.ons.ssdc.caseprocessor.rasrm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.rasrm.client.RasRmCaseServiceClient;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmCaseGroupDTO;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmCaseIacResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmCaseResponseDTO;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;

@ExtendWith(MockitoExtension.class)
class RasRmCaseIacServiceTest {
  @Mock private RasRmCaseServiceClient rasRmCaseServiceClient;

  @InjectMocks private RasRmCaseIacService underTest;

  @Test
  void getRasRmIac() {
    UUID rasRmCollectionExerciseId = UUID.randomUUID();
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setMetadata(
        Map.of("rasRmCollectionExerciseId", rasRmCollectionExerciseId.toString()));
    Case caze = new Case();
    caze.setCollectionExercise(collectionExercise);
    UUID rasRmPartyId = UUID.randomUUID();
    caze.setSample(Map.of("partyId", rasRmPartyId.toString()));

    RasRmCaseGroupDTO rasRmCaseGroupDto = new RasRmCaseGroupDTO();
    rasRmCaseGroupDto.setCollectionExerciseId(rasRmCollectionExerciseId);

    UUID rasRmCaseId = UUID.randomUUID();
    RasRmCaseResponseDTO rasRmCaseResponseDto = new RasRmCaseResponseDTO();
    rasRmCaseResponseDto.setId(rasRmCaseId);
    rasRmCaseResponseDto.setCaseGroup(rasRmCaseGroupDto);

    RasRmCaseResponseDTO[] rasRmCaseResponseDtos =
        new RasRmCaseResponseDTO[] {rasRmCaseResponseDto};

    RasRmCaseIacResponseDTO rasRmCaseIacResponseDto = new RasRmCaseIacResponseDTO();
    rasRmCaseIacResponseDto.setIac("test IAC");
    RasRmCaseIacResponseDTO[] rasRmCaseIacResponseDtos =
        new RasRmCaseIacResponseDTO[] {rasRmCaseIacResponseDto};

    when(rasRmCaseServiceClient.getCases(rasRmPartyId)).thenReturn(rasRmCaseResponseDtos);
    when(rasRmCaseServiceClient.getIacs(rasRmCaseId)).thenReturn(rasRmCaseIacResponseDtos);

    String actualResult = underTest.getRasRmIac(caze);

    assertThat(actualResult).isEqualTo("test IAC");
  }

  @Test
  public void testMetaObjectNull() {
    Case caze = new Case();
    caze.setCollectionExercise(new CollectionExercise());

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> underTest.getRasRmIac(caze));

    assertThat(thrown.getMessage())
        .isEqualTo("Unexpected null metadata. Metadata is required for RAS-RM business.");
  }

  @Test
  public void testNotContainingCorrectMetaDataType() {
    Case caze = new Case();
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setMetadata(Map.of("favourtistColour", "Blue"));
    caze.setCollectionExercise(collectionExercise);

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> underTest.getRasRmIac(caze));

    assertThat(thrown.getMessage())
        .isEqualTo("Metadata does not contain mandatory rasRmCollectionExerciseId");
  }

  @Test
  public void testMetaDataObjectNotMap() {
    Case caze = new Case();
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setMetadata("Hello World, type Stirngy McString");
    caze.setCollectionExercise(collectionExercise);

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> underTest.getRasRmIac(caze));

    assertThat(thrown.getMessage())
        .isEqualTo("Unexpected metadata type. Wanted Map but got String");
  }

  @Test
  public void testEmptyRMIACSException() {
    UUID rasRmCollectionExerciseId = UUID.randomUUID();
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setMetadata(
        Map.of("rasRmCollectionExerciseId", rasRmCollectionExerciseId.toString()));
    Case caze = new Case();
    caze.setCollectionExercise(collectionExercise);
    UUID rasRmPartyId = UUID.randomUUID();
    caze.setSample(Map.of("partyId", rasRmPartyId.toString()));

    RasRmCaseGroupDTO rasRmCaseGroupDto = new RasRmCaseGroupDTO();
    rasRmCaseGroupDto.setCollectionExerciseId(rasRmCollectionExerciseId);

    UUID rasRmCaseId = UUID.randomUUID();
    RasRmCaseResponseDTO rasRmCaseResponseDto = new RasRmCaseResponseDTO();
    rasRmCaseResponseDto.setId(rasRmCaseId);
    rasRmCaseResponseDto.setCaseGroup(rasRmCaseGroupDto);

    RasRmCaseResponseDTO[] rasRmCaseResponseDtos =
        new RasRmCaseResponseDTO[] {rasRmCaseResponseDto};

    RasRmCaseIacResponseDTO rasRmCaseIacResponseDto = new RasRmCaseIacResponseDTO();
    rasRmCaseIacResponseDto.setIac("test IAC");
    RasRmCaseIacResponseDTO[] rasRmCaseIacResponseDtos = new RasRmCaseIacResponseDTO[] {};

    when(rasRmCaseServiceClient.getCases(rasRmPartyId)).thenReturn(rasRmCaseResponseDtos);
    when(rasRmCaseServiceClient.getIacs(rasRmCaseId)).thenReturn(rasRmCaseIacResponseDtos);

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> underTest.getRasRmIac(caze));

    assertThat(thrown.getMessage()).isEqualTo("RAS RM has not made any IAC available for our case");
  }
}
