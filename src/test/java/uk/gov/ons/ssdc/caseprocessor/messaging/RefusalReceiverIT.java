package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;

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
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
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
import uk.gov.ons.ssdc.caseprocessor.utils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.utils.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RefusalReceiverIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String RH_CASE_QUEUE = "case.rh.case";

  @Value("${queueconfig.refusal-response-queue}")
  private String inboundRefusalQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private CaseRepository caseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private WaveOfContactRepository waveOfContactRepository;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundRefusalQueue);
    rabbitQueueHelper.purgeQueue(RH_CASE_QUEUE);
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    waveOfContactRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
  }

  @Test
  public void testRefusal() throws Exception {
    try (QueueSpy rhCaseQueueSpy = rabbitQueueHelper.listen(RH_CASE_QUEUE)) {
      // GIVEN

      CollectionExercise collectionExercise = new CollectionExercise();
      collectionExercise.setId(UUID.randomUUID());
      collectionExerciseRepository.saveAndFlush(collectionExercise);

      Case caze = new Case();
      caze.setId(TEST_CASE_ID);
      caze.setCollectionExercise(collectionExercise);
      caze.setRefusalReceived(null);

      caseRepository.saveAndFlush(caze);

      RefusalDTO refusalDTO = new RefusalDTO();
      CollectionCase collectionCase = new CollectionCase();
      collectionCase.setCaseId(TEST_CASE_ID);
      refusalDTO.setCollectionCase(collectionCase);
      refusalDTO.setType(RefusalTypeDTO.EXTRAORDINARY_REFUSAL);
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setRefusal(refusalDTO);
      ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
      responseManagementEvent.setPayload(payloadDTO);

      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.REFUSAL_RECEIVED);
      responseManagementEvent.setEvent(eventDTO);

      rabbitQueueHelper.sendMessage(inboundRefusalQueue, responseManagementEvent);

      //  THEN
      ResponseManagementEvent actualResponseManagementEvent =
          rhCaseQueueSpy.checkExpectedMessageReceived();

      CollectionCase emittedCase = actualResponseManagementEvent.getPayload().getCollectionCase();
      assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);
      assertThat(emittedCase.getRefusalReceived()).isEqualTo(RefusalTypeDTO.EXTRAORDINARY_REFUSAL);

      assertThat(eventRepository.findAll().size()).isEqualTo(1);
      Event event = eventRepository.findAll().get(0);
      assertThat(event.getCaze().getId()).isEqualTo(TEST_CASE_ID);
      assertThat(event.getEventType()).isEqualTo(EventType.REFUSAL_RECEIVED);
    }
  }
}
