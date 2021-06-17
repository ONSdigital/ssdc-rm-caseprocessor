package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
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
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.WaveOfContactRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class InvalidAddressIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();

  @Value("${queueconfig.outbound-case-queue}")
  private String outboundCaseQueue;

  @Value("${queueconfig.invalid-address-queue}")
  private String inboundInvalidAddressQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private CaseRepository caseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private WaveOfContactRepository waveOfContactRepository;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundInvalidAddressQueue);
    rabbitQueueHelper.purgeQueue(outboundCaseQueue);
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    waveOfContactRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
  }

  @Test
  public void testInvalidAddress() throws Exception {
    try (QueueSpy outboundCaseQueueSpy = rabbitQueueHelper.listen(outboundCaseQueue)) {
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
      rabbitQueueHelper.sendMessage(inboundInvalidAddressQueue, responseManagementEvent);

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
