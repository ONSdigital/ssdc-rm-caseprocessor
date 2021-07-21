package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@ExtendWith(MockitoExtension.class)
public class SurveyReceiverTest {

  private final UUID TEST_CASE_ID = UUID.randomUUID();
  private final String TEST_QID_ID = "1234567890123456";
  private final String TEST_AGENT_ID = "any agent";

  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;
  @Mock private CaseService caseService;

  @InjectMocks SurveyLaunchedReceiver underTest;

  @Test
  public void testSurveryLaunchedEventFromRH() {
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setDateTime(OffsetDateTime.now());
    managementEvent.getEvent().setType(EventTypeDTO.SURVEY_LAUNCHED);
    managementEvent.getEvent().setChannel("RH");
    managementEvent.setPayload(new PayloadDTO());

    ResponseDTO response = new ResponseDTO();
    response.setCaseId(TEST_CASE_ID);
    response.setQuestionnaireId(TEST_QID_ID);
    response.setAgentId(TEST_AGENT_ID);
    managementEvent.getPayload().setResponse(response);

    UacQidLink expectedUacQidLink = new UacQidLink();
    expectedUacQidLink.setUac(TEST_QID_ID);
    Case caze = new Case();
    caze.setId(TEST_CASE_ID);
    expectedUacQidLink.setCaze(caze);

    // Given
    when(uacService.findByQid(TEST_QID_ID)).thenReturn(expectedUacQidLink);

    // when
    underTest.receiveMessage(managementEvent);

    // then
    verify(uacService).findByQid(TEST_QID_ID);

    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseService).saveCaseAndEmitCaseUpdatedEvent(caseArgumentCaptor.capture());
    assertThat(caseArgumentCaptor.getValue().getId()).isEqualTo(TEST_CASE_ID);
    assertThat(caseArgumentCaptor.getValue().isSurveyLaunched()).isTrue();

    verify(eventLogger)
        .logUacQidEvent(
            eq(expectedUacQidLink),
            any(OffsetDateTime.class),
            eq("Survey launched"),
            eq(EventType.SURVEY_LAUNCHED),
            eq(managementEvent.getEvent()),
            any(),
            any());

    verifyNoMoreInteractions(uacService);
    verifyNoMoreInteractions(caseService);
    verifyNoMoreInteractions(eventLogger);
  }

  @Test
  public void testRespondentAuthenticatedEventTypeLoggedAndRejected() {
    // Given
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setDateTime(OffsetDateTime.now());
    managementEvent.getEvent().setType(EventTypeDTO.RESPONDENT_AUTHENTICATED);
    managementEvent.setPayload(new PayloadDTO());

    ResponseDTO response = new ResponseDTO();
    response.setCaseId(TEST_CASE_ID);
    response.setQuestionnaireId(TEST_QID_ID);
    response.setAgentId(TEST_AGENT_ID);
    managementEvent.getPayload().setResponse(response);

    UacQidLink expectedUacQidLink = new UacQidLink();
    expectedUacQidLink.setId(TEST_CASE_ID);
    expectedUacQidLink.setUac(TEST_QID_ID);
    OffsetDateTime messageTimestamp = OffsetDateTime.now();

    // Given
    when(uacService.findByQid(TEST_QID_ID)).thenReturn(expectedUacQidLink);

    // when
    underTest.receiveMessage(managementEvent);

    // then
    InOrder inOrder = inOrder(uacService, eventLogger);

    inOrder.verify(uacService).findByQid(TEST_QID_ID);

    ArgumentCaptor<String> respondentAuthenticatedCaptor = ArgumentCaptor.forClass(String.class);
    inOrder
        .verify(eventLogger)
        .logUacQidEvent(
            eq(expectedUacQidLink),
            any(OffsetDateTime.class),
            eq("Respondent authenticated"),
            eq(EventType.RESPONDENT_AUTHENTICATED),
            eq(managementEvent.getEvent()),
            eq(response),
            any());

    verifyNoMoreInteractions(uacService);
    verifyNoMoreInteractions(eventLogger);
    verifyNoInteractions(caseService);
  }

  @Test
  public void testInvalidSurveyEventTypeException() {
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setType(EventTypeDTO.CASE_CREATED);

    String expectedErrorMessage =
        String.format("Event Type '%s' is invalid on this topic", EventTypeDTO.CASE_CREATED);

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          try {
            // WHEN
            underTest.receiveMessage(managementEvent);
          } catch (RuntimeException re) {
            // THEN
            assertThat(re.getMessage()).isEqualTo(expectedErrorMessage);
            throw re;
          }
        });
  }
}
