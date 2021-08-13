package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.InvalidCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class InvalidCaseReceiverIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String INBOUND_INVALID_CASE_TOPIC = "event_invalid-case";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testInvalidCase() throws Exception {
    try (QueueSpy<EventDTO> outboundCaseQueueSpy =
        pubsubHelper.listen(OUTBOUND_CASE_SUBSCRIPTION, EventDTO.class)) {
      // GIVEN

      CollectionExercise collectionExercise = new CollectionExercise();
      collectionExercise.setId(UUID.randomUUID());
      collectionExerciseRepository.saveAndFlush(collectionExercise);

      Case caze = new Case();
      caze.setId(TEST_CASE_ID);
      caze.setCollectionExercise(collectionExercise);
      caze.setInvalid(false);

      caseRepository.saveAndFlush(caze);

      InvalidCase invalidCase = new InvalidCase();
      invalidCase.setCaseId(TEST_CASE_ID);
      invalidCase.setReason("Not found");
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setInvalidCase(invalidCase);
      EventDTO event = new EventDTO();
      event.setPayload(payloadDTO);

      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setTopic(INBOUND_INVALID_CASE_TOPIC);
      eventHeader.setSource("RH");
      eventHeader.setDateTime(OffsetDateTime.now());
      eventHeader.setChannel("RH");
      eventHeader.setMessageId(UUID.randomUUID());
      event.setHeader(eventHeader);

      //  When
      pubsubHelper.sendMessage(INBOUND_INVALID_CASE_TOPIC, event);

      //  Then
      EventDTO actualEvent = outboundCaseQueueSpy.checkExpectedMessageReceived();

      CaseUpdateDTO emittedCase = actualEvent.getPayload().getCaseUpdate();
      assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);
      assertThat(emittedCase.isInvalid()).isTrue();

      assertThat(eventRepository.findAll().size()).isEqualTo(1);
      Event databaseEvent = eventRepository.findAll().get(0);
      assertThat(databaseEvent.getCaze().getId()).isEqualTo(TEST_CASE_ID);
      assertThat(databaseEvent.getType()).isEqualTo(EventType.INVALID_CASE);
    }
  }
}
