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
import uk.gov.ons.ssdc.caseprocessor.model.dto.CollectionCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.InvalidAddress;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
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
public class InvalidAddressIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String INBOUND_INVALID_ADDRESS_TOPIC = "event.invalid";

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
  public void testInvalidAddress() throws Exception {
    try (QueueSpy<ResponseManagementEvent> outboundCaseQueueSpy =
        pubsubHelper.listen(OUTBOUND_CASE_SUBSCRIPTION, ResponseManagementEvent.class)) {
      // GIVEN

      CollectionExercise collectionExercise = new CollectionExercise();
      collectionExercise.setId(UUID.randomUUID());
      collectionExerciseRepository.saveAndFlush(collectionExercise);

      Case caze = new Case();
      caze.setId(TEST_CASE_ID);
      caze.setCollectionExercise(collectionExercise);
      caze.setAddressInvalid(false);

      caseRepository.saveAndFlush(caze);

      InvalidAddress invalidAddress = new InvalidAddress();
      invalidAddress.setCaseId(TEST_CASE_ID);
      invalidAddress.setReason("Not found");
      invalidAddress.setNotes("Looked hard");
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setInvalidAddress(invalidAddress);
      ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
      responseManagementEvent.setPayload(payloadDTO);

      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.ADDRESS_NOT_VALID);
      eventDTO.setSource("RH");
      eventDTO.setDateTime(OffsetDateTime.now());
      eventDTO.setChannel("RH");
      eventDTO.setTransactionId(UUID.randomUUID());
      responseManagementEvent.setEvent(eventDTO);

      //  When
      pubsubHelper.sendMessage(INBOUND_INVALID_ADDRESS_TOPIC, responseManagementEvent);

      //  Then
      ResponseManagementEvent actualResponseManagementEvent =
          outboundCaseQueueSpy.checkExpectedMessageReceived();

      CollectionCase emittedCase = actualResponseManagementEvent.getPayload().getCollectionCase();
      assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);
      assertThat(emittedCase.isInvalidAddress()).isTrue();

      assertThat(eventRepository.findAll().size()).isEqualTo(1);
      Event event = eventRepository.findAll().get(0);
      assertThat(event.getCaze().getId()).isEqualTo(TEST_CASE_ID);
      assertThat(event.getEventType()).isEqualTo(EventType.ADDRESS_NOT_VALID);
    }
  }
}
