package uk.gov.ons.ssdc.caseprocessor.rasrm.service;

import static org.assertj.core.api.Assertions.assertThat;
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
}
