package uk.gov.ons.ssdc.caseprocessor.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CollectionCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.RefusalType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {

  @Mock CaseRepository caseRepository;
  @Mock MessageSender messageSender;

  @InjectMocks CaseService underTest;

  @Test
  public void saveCaseAndEmitCaseUpdatedEvent() {
    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    caze.setSample(Map.of("foo", "bar"));
    caze.setReceiptReceived(true);
    caze.setAddressInvalid(true);
    caze.setSurveyLaunched(true);
    caze.setRefusalReceived(RefusalType.HARD_REFUSAL);

    underTest.saveCaseAndEmitCaseUpdatedEvent(caze);
    verify(caseRepository).saveAndFlush(caze);

    ArgumentCaptor<ResponseManagementEvent> responseManagementEventArgumentCaptor =
        ArgumentCaptor.forClass(ResponseManagementEvent.class);

    verify(messageSender).sendMessage(any(), responseManagementEventArgumentCaptor.capture());
    ResponseManagementEvent actualResponeManagementEvent = responseManagementEventArgumentCaptor.getValue();

    CollectionCase actualCollectionCase = actualResponeManagementEvent.getPayload().getCollectionCase();
    assertThat(actualCollectionCase.getCaseId()).isEqualTo(caze.getId());
    assertThat(actualCollectionCase.getSample()).isEqualTo(caze.getSample());
    assertThat(actualCollectionCase.isReceiptReceived()).isTrue();
    assertThat(actualCollectionCase.isInvalidAddress()).isTrue();
    assertThat(actualCollectionCase.isSurveyLaunched()).isTrue();
    assertThat(actualCollectionCase.getRefusalReceived()).isEqualTo(RefusalTypeDTO.HARD_REFUSAL);

    assertThat(actualResponeManagementEvent.getEvent().getType()).isEqualTo(EventTypeDTO.CASE_UPDATED);
  }

  @Test
  public void saveCase() {
    Case caze = new Case();
    underTest.saveCase(caze);
    verify(caseRepository).saveAndFlush(caze);
  }

  @Test
  public void emitCaseCreatedEvent() {
    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    caze.setSample(Map.of("foo", "bar"));
    caze.setReceiptReceived(true);
    caze.setAddressInvalid(true);
    caze.setSurveyLaunched(true);
    caze.setRefusalReceived(RefusalType.EXTRAORDINARY_REFUSAL);

    underTest.emitCaseCreatedEvent(caze);

    ArgumentCaptor<ResponseManagementEvent> responseManagementEventArgumentCaptor =
            ArgumentCaptor.forClass(ResponseManagementEvent.class);

    verify(messageSender).sendMessage(any(), responseManagementEventArgumentCaptor.capture());
    ResponseManagementEvent actualResponeManagementEvent = responseManagementEventArgumentCaptor.getValue();

    CollectionCase actualCollectionCase = actualResponeManagementEvent.getPayload().getCollectionCase();
    assertThat(actualCollectionCase.getCaseId()).isEqualTo(caze.getId());
    assertThat(actualCollectionCase.getSample()).isEqualTo(caze.getSample());
    assertThat(actualCollectionCase.isReceiptReceived()).isTrue();
    assertThat(actualCollectionCase.isInvalidAddress()).isTrue();
    assertThat(actualCollectionCase.isSurveyLaunched()).isTrue();
    assertThat(actualCollectionCase.getRefusalReceived()).isEqualTo(RefusalTypeDTO.EXTRAORDINARY_REFUSAL);

    assertThat(actualResponeManagementEvent.getEvent().getType()).isEqualTo(EventTypeDTO.CASE_CREATED);
  }

  @Test
  public void getCaseByCaseId() {
    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    Optional<Case> caseOpt = Optional.of(caze);
    when(caseRepository.findById(any())).thenReturn(caseOpt);

    Case returnedCase = underTest.getCaseByCaseId(caze.getId());
    assertThat(returnedCase).isEqualTo(caze);
    verify(caseRepository).findById(caze.getId());
  }

  @Test
  public void getByCaseIdMissingCase() {
    UUID caseId = UUID.randomUUID();
    String expectedErrorMessage = String.format("Case ID '%s' not present", caseId);

    RuntimeException thrown =
            assertThrows(RuntimeException.class, () -> underTest.getCaseByCaseId(caseId));

    Assertions.assertThat(thrown.getMessage()).isEqualTo(expectedErrorMessage);
  }

}
