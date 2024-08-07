package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.RefusalType;
import uk.gov.ons.ssdc.common.model.entity.Survey;

@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {
  @Mock CaseRepository caseRepository;
  @Mock MessageSender messageSender;

  @InjectMocks CaseService underTest;

  @Test
  public void saveCaseAndEmitCaseUpdatedEvent() {
    ReflectionTestUtils.setField(underTest, "caseUpdateTopic", "Test topic");
    ReflectionTestUtils.setField(underTest, "pubsubProject", "Test project");

    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    CollectionExercise collex = new CollectionExercise();
    collex.setId(UUID.randomUUID());
    collex.setSurvey(survey);

    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    caze.setCaseRef(1234567890L);
    caze.setCollectionExercise(collex);
    caze.setSample(Map.of("foo", "bar"));
    caze.setSampleSensitive(Map.of("Top", "Secret"));
    caze.setInvalid(true);
    caze.setRefusalReceived(RefusalType.HARD_REFUSAL);

    underTest.saveCaseAndEmitCaseUpdate(caze, TEST_CORRELATION_ID, TEST_ORIGINATING_USER);
    verify(caseRepository).saveAndFlush(caze);

    ArgumentCaptor<EventDTO> eventArgumentCaptor = ArgumentCaptor.forClass(EventDTO.class);

    verify(messageSender).sendMessage(any(), eventArgumentCaptor.capture());
    EventDTO actualEvent = eventArgumentCaptor.getValue();

    assertThat(actualEvent.getHeader().getTopic()).isEqualTo("Test topic");
    assertThat(actualEvent.getHeader().getCorrelationId()).isEqualTo(TEST_CORRELATION_ID);
    assertThat(actualEvent.getHeader().getOriginatingUser()).isEqualTo(TEST_ORIGINATING_USER);

    CaseUpdateDTO actualCaseUpdate = actualEvent.getPayload().getCaseUpdate();
    assertThat(actualCaseUpdate.getCaseId()).isEqualTo(caze.getId());
    assertThat(actualCaseUpdate.getCaseRef()).isEqualTo(caze.getCaseRef().toString());
    assertThat(actualCaseUpdate.getCollectionExerciseId()).isEqualTo(collex.getId());
    assertThat(actualCaseUpdate.getSurveyId()).isEqualTo(survey.getId());
    assertThat(actualCaseUpdate.getSample()).isEqualTo(caze.getSample());
    assertThat(actualCaseUpdate.isInvalid()).isTrue();
    assertThat(actualCaseUpdate.getRefusalReceived()).isEqualTo(RefusalTypeDTO.HARD_REFUSAL);
    assertThat(actualCaseUpdate.getSampleSensitive()).isEqualTo(Map.of("Top", "REDACTED"));
  }

  @Test
  public void saveCase() {
    Case caze = new Case();
    underTest.saveCase(caze);
    verify(caseRepository).saveAndFlush(caze);
  }

  @Test
  public void emitCaseUpdatedEvent() {
    ReflectionTestUtils.setField(underTest, "caseUpdateTopic", "Test topic");
    ReflectionTestUtils.setField(underTest, "pubsubProject", "Test project");

    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    CollectionExercise collex = new CollectionExercise();
    collex.setId(UUID.randomUUID());
    collex.setSurvey(survey);

    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    caze.setCaseRef(1234567890L);
    caze.setCollectionExercise(collex);
    caze.setSample(Map.of("foo", "bar"));
    caze.setInvalid(true);
    caze.setRefusalReceived(RefusalType.EXTRAORDINARY_REFUSAL);
    caze.setCreatedAt(OffsetDateTime.now().minusSeconds(10));
    caze.setLastUpdatedAt(OffsetDateTime.now());

    underTest.emitCaseUpdate(caze, TEST_CORRELATION_ID, TEST_ORIGINATING_USER);

    ArgumentCaptor<EventDTO> eventArgumentCaptor = ArgumentCaptor.forClass(EventDTO.class);

    verify(messageSender).sendMessage(any(), eventArgumentCaptor.capture());
    EventDTO actualEvent = eventArgumentCaptor.getValue();

    assertThat(actualEvent.getHeader().getTopic()).isEqualTo("Test topic");
    assertThat(actualEvent.getHeader().getCorrelationId()).isEqualTo(TEST_CORRELATION_ID);
    assertThat(actualEvent.getHeader().getOriginatingUser()).isEqualTo(TEST_ORIGINATING_USER);

    CaseUpdateDTO actualCaseUpdate = actualEvent.getPayload().getCaseUpdate();
    assertThat(actualCaseUpdate.getCaseId()).isEqualTo(caze.getId());
    assertThat(actualCaseUpdate.getCaseRef()).isEqualTo(caze.getCaseRef().toString());
    assertThat(actualCaseUpdate.getCollectionExerciseId()).isEqualTo(collex.getId());
    assertThat(actualCaseUpdate.getSurveyId()).isEqualTo(survey.getId());
    assertThat(actualCaseUpdate.getSample()).isEqualTo(caze.getSample());
    assertThat(actualCaseUpdate.isInvalid()).isTrue();
    assertThat(actualCaseUpdate.getRefusalReceived())
        .isEqualTo(RefusalTypeDTO.EXTRAORDINARY_REFUSAL);
    assertThat(actualCaseUpdate.getCreatedAt()).isEqualTo(caze.getCreatedAt());
    assertThat(actualCaseUpdate.getLastUpdatedAt()).isEqualTo(caze.getLastUpdatedAt());
  }

  @Test
  public void getCaseByCaseId() {
    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    Optional<Case> caseOpt = Optional.of(caze);
    when(caseRepository.findById(any())).thenReturn(caseOpt);

    Case returnedCase = underTest.getCase(caze.getId());
    assertThat(returnedCase).isEqualTo(caze);
    verify(caseRepository).findById(caze.getId());
  }

  @Test
  public void getByCaseIdMissingCase() {
    UUID caseId = UUID.randomUUID();
    String expectedErrorMessage = String.format("Case with ID '%s' not found", caseId);

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.getCase(caseId));

    assertThat(thrown.getMessage()).isEqualTo(expectedErrorMessage);
  }
}
