package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.NewCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.EventType;

@ExtendWith(MockitoExtension.class)
public class NewCaseReceiverTest {

  private final UUID TEST_CASE_ID = UUID.randomUUID();
  private final UUID TEST_CASE_COLLECTION_EXERCISE_ID = UUID.randomUUID();
  private final UUID TEST_JOB_ID = UUID.randomUUID();
  private static final byte[] caserefgeneratorkey =
      new byte[] {0x10, 0x20, 0x10, 0x20, 0x10, 0x20, 0x10, 0x20};

  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;
  @Mock private CaseService caseService;
  @Mock private CaseRepository caseRepository;
  @Mock private CollectionExerciseRepository collectionExerciseRepository;

  @InjectMocks NewCaseReceiver underTest;

  @Test
  public void testNewCaseReceiver() {
    // Given
    NewCase newCase = new NewCase();
    newCase.setCaseId(TEST_CASE_ID);
    newCase.setCollectionExerciseId(TEST_CASE_COLLECTION_EXERCISE_ID);

    Map<String, String> sample = new HashMap<>();
    sample.put("ADDRESS_LINE1", "123 Fake Street");
    sample.put("POSTCODE", "NP10 111");
    newCase.setSample(sample);

    Map<String, String> sampleSensitive = new HashMap<>();
    sample.put("Telephone", "02071234567");
    newCase.setSampleSensitive(sampleSensitive);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    eventHeader.setCorrelationId(TEST_CORRELATION_ID);
    eventHeader.setOriginatingUser(TEST_ORIGINATING_USER);
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setNewCase(newCase);

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    Message<byte[]> eventMessage = constructMessageWithValidTimeStamp(event);

    when(caseRepository.existsById(TEST_CASE_ID)).thenReturn(false);

    CollectionExercise collex = new CollectionExercise();
    Optional<CollectionExercise> collexOpt = Optional.of(collex);
    when(collectionExerciseRepository.findById(TEST_CASE_COLLECTION_EXERCISE_ID))
        .thenReturn(collexOpt);

    when(caseRepository.saveAndFlush(any(Case.class)))
        .then(
            invocation -> {
              Case caze = invocation.getArgument(0);
              caze.setSecretSequenceNumber(123);
              return caze;
            });

    ReflectionTestUtils.setField(underTest, "caserefgeneratorkey", caserefgeneratorkey);

    // When
    underTest.receiveNewCase(eventMessage);

    // Then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseService)
        .emitCaseUpdate(
            caseArgumentCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    Case actualCase = caseArgumentCaptor.getValue();
    assertThat(actualCase.getId()).isEqualTo(TEST_CASE_ID);

    verify(eventLogger)
        .logCaseEvent(
            actualCase,
            event.getHeader().getDateTime(),
            "New case created",
            EventType.NEW_CASE,
            event.getHeader(),
            newCase,
            getMsgTimeStamp(eventMessage));
  }

  @Test
  public void testNewCaseReceiverCaseAlreadyExists() {
    // Given
    NewCase newCase = new NewCase();
    newCase.setCaseId(TEST_CASE_ID);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setNewCase(newCase);

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    byte[] payloadBytes = JsonHelper.convertObjectToJson(event).getBytes();
    Message<byte[]> message = mock(Message.class);
    when(message.getPayload()).thenReturn(payloadBytes);

    when(caseRepository.existsById(TEST_CASE_ID)).thenReturn(true);

    // When
    underTest.receiveNewCase(message);

    // Then
    verify(caseService, never()).emitCaseUpdate(any(), any(UUID.class), anyString());
    verifyNoInteractions(eventLogger);
  }

  @Test
  public void testNewCaseReceiverCollectionExerciseNotFound() {
    // Given
    NewCase newCase = new NewCase();
    newCase.setCaseId(TEST_CASE_ID);
    newCase.setCollectionExerciseId(TEST_CASE_COLLECTION_EXERCISE_ID);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setNewCase(newCase);

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    Message<byte[]> eventMessage = constructMessageWithValidTimeStamp(event);

    when(caseRepository.existsById(TEST_CASE_ID)).thenReturn(false);
    when(collectionExerciseRepository.findById(TEST_CASE_COLLECTION_EXERCISE_ID))
        .thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> underTest.receiveNewCase(eventMessage));
    verifyNoInteractions(eventLogger);
  }
}
