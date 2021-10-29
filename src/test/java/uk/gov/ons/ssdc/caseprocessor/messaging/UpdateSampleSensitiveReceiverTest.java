package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSampleSensitive;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.Survey;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;
import uk.gov.ons.ssdc.common.validation.LengthRule;
import uk.gov.ons.ssdc.common.validation.Rule;

@ExtendWith(MockitoExtension.class)
class UpdateSampleSensitiveReceiverTest {

  @Mock private CaseService caseService;
  @Mock private EventLogger eventLogger;

  @InjectMocks UpdateSampleSensitiveReceiver underTest;

  @Test
  void testUpdateSampleSensitiveReceiver() {
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")).minusHours(1));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("CC");
    managementEvent.getHeader().setCorrelationId(TEST_CORRELATION_ID);
    managementEvent.getHeader().setOriginatingUser(TEST_ORIGINATING_USER);
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setUpdateSampleSensitive(new UpdateSampleSensitive());
    managementEvent.getPayload().getUpdateSampleSensitive().setCaseId(UUID.randomUUID());
    managementEvent
        .getPayload()
        .getUpdateSampleSensitive()
        .setSampleSensitive(Map.of("PHONE_NUMBER", "9999999"));
    Message<byte[]> message = constructMessage(managementEvent);

    // Given
    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    survey.setSampleValidationRules(
        new ColumnValidator[] {
          new ColumnValidator("PHONE_NUMBER", true, new Rule[] {new LengthRule(30)})
        });
    CollectionExercise collex = new CollectionExercise();
    collex.setId(UUID.randomUUID());
    collex.setSurvey(survey);

    Case expectedCase = new Case();
    expectedCase.setCollectionExercise(collex);
    Map<String, String> sensitiveData = new HashMap<>();
    sensitiveData.put("PHONE_NUMBER", "1111111");
    expectedCase.setSampleSensitive(sensitiveData);
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // when
    underTest.receiveMessage(message);

    // then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);

    verify(caseService)
        .saveCaseAndEmitCaseUpdate(
            caseArgumentCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    Case actualCase = caseArgumentCaptor.getValue();
    assertThat(actualCase.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", "9999999"));

    verify(eventLogger)
        .logCaseEvent(
            eq(expectedCase),
            eq("Sensitive data updated"),
            eq(EventType.UPDATE_SAMPLE_SENSITIVE),
            eq(managementEvent),
            eq(message));
  }

  @Test
  void testUpdateSampleSensitiveReceiverBlankingIsAllowed() {
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")).minusHours(1));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("CC");
    managementEvent.getHeader().setCorrelationId(TEST_CORRELATION_ID);
    managementEvent.getHeader().setOriginatingUser(TEST_ORIGINATING_USER);
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setUpdateSampleSensitive(new UpdateSampleSensitive());
    managementEvent.getPayload().getUpdateSampleSensitive().setCaseId(UUID.randomUUID());
    managementEvent
        .getPayload()
        .getUpdateSampleSensitive()
        .setSampleSensitive(Map.of("PHONE_NUMBER", ""));
    Message<byte[]> message = constructMessage(managementEvent);

    // Given
    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    survey.setSampleValidationRules(
        new ColumnValidator[] {
          new ColumnValidator("PHONE_NUMBER", true, new Rule[] {new LengthRule(30)})
        });
    CollectionExercise collex = new CollectionExercise();
    collex.setId(UUID.randomUUID());
    collex.setSurvey(survey);

    Case expectedCase = new Case();
    expectedCase.setCollectionExercise(collex);
    Map<String, String> sensitiveData = new HashMap<>();
    sensitiveData.put("PHONE_NUMBER", "1111111");
    expectedCase.setSampleSensitive(sensitiveData);
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // when
    underTest.receiveMessage(message);

    // then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);

    verify(caseService)
        .saveCaseAndEmitCaseUpdate(
            caseArgumentCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    Case actualCase = caseArgumentCaptor.getValue();
    assertThat(actualCase.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", ""));

    verify(eventLogger)
        .logCaseEvent(
            eq(expectedCase),
            eq("Sensitive data updated"),
            eq(EventType.UPDATE_SAMPLE_SENSITIVE),
            eq(managementEvent),
            eq(message));
  }

  @Test
  void testMessageKeyDoesNotMatchExistingEntry() {
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")).minusHours(1));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setUpdateSampleSensitive(new UpdateSampleSensitive());
    managementEvent.getPayload().getUpdateSampleSensitive().setCaseId(UUID.randomUUID());
    managementEvent
        .getPayload()
        .getUpdateSampleSensitive()
        .setSampleSensitive(Map.of("UPRN", "9999999"));
    Message<byte[]> message = constructMessage(managementEvent);

    // Given
    Case expectedCase = new Case();
    Map<String, String> sensitiveData = new HashMap<>();
    sensitiveData.put("PHONE_NUMBER", "1111111");
    expectedCase.setSampleSensitive(sensitiveData);
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // When, then throws
    assertThrows(RuntimeException.class, () -> underTest.receiveMessage(message));

    verify(caseService, never()).saveCase(any());
    verify(eventLogger, never()).logCaseEvent(any(), any(), any(), any(), any(Message.class));
  }

  @Test
  void testUpdateSampleSensitiveReceiverFailsValidation() {
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")).minusHours(1));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setUpdateSampleSensitive(new UpdateSampleSensitive());
    managementEvent.getPayload().getUpdateSampleSensitive().setCaseId(UUID.randomUUID());
    managementEvent
        .getPayload()
        .getUpdateSampleSensitive()
        .setSampleSensitive(Map.of("PHONE_NUMBER", "123456789"));
    Message<byte[]> message = constructMessage(managementEvent);

    // Given
    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    survey.setSampleValidationRules(
        new ColumnValidator[] {
          new ColumnValidator("PHONE_NUMBER", true, new Rule[] {new LengthRule(3)})
        });
    CollectionExercise collex = new CollectionExercise();
    collex.setId(UUID.randomUUID());
    collex.setSurvey(survey);

    Case expectedCase = new Case();
    expectedCase.setCollectionExercise(collex);
    Map<String, String> sensitiveData = new HashMap<>();
    sensitiveData.put("PHONE_NUMBER", "123");
    expectedCase.setSampleSensitive(sensitiveData);
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // When, then throws
    assertThrows(RuntimeException.class, () -> underTest.receiveMessage(message));

    verify(caseService, never()).saveCase(any());
    verify(eventLogger, never()).logCaseEvent(any(), any(), any(), any(), any(Message.class));
  }
}
