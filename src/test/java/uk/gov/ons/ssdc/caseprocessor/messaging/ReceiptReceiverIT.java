package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
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
public class ReceiptReceiverIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String TEST_QID = "123456";
  private static final UUID TEST_UACLINK_ID = UUID.randomUUID();
  private static final String RH_CASE_QUEUE = "case.rh.case";
  private static final String RH_UAC_QUEUE = "case.rh.uac";

  @Value("${queueconfig.receipt-response-queue}")
  private String inboundReceiptQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private CaseRepository caseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private WaveOfContactRepository waveOfContactRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundReceiptQueue);
    rabbitQueueHelper.purgeQueue(RH_CASE_QUEUE);
    rabbitQueueHelper.purgeQueue(RH_UAC_QUEUE);
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    waveOfContactRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
  }

  @Test
  public void testReceipt() throws Exception {
    try (QueueSpy rhUacQueueSpy = rabbitQueueHelper.listen(RH_UAC_QUEUE);
        QueueSpy rhCaseQueueSpy = rabbitQueueHelper.listen(RH_CASE_QUEUE)) {
      // GIVEN

      CollectionExercise collectionExercise = new CollectionExercise();
      collectionExercise.setId(UUID.randomUUID());
      collectionExerciseRepository.saveAndFlush(collectionExercise);

      Case caze = new Case();
      caze.setId(TEST_CASE_ID);
      caze.setCollectionExercise(collectionExercise);

      Map<String, String> sample = new HashMap<>();
      sample.put("Address", "Tenby");
      sample.put("Org", "Brewery");
      caze.setReceiptReceived(false);
      caze.setSample(sample);

      caseRepository.saveAndFlush(caze);

      UacQidLink uacQidLink = new UacQidLink();
      uacQidLink.setId(TEST_UACLINK_ID);
      uacQidLink.setQid(TEST_QID);
      uacQidLink.setCaze(caze);
      uacQidLink.setActive(true);
      uacQidLinkRepository.saveAndFlush(uacQidLink);

      ResponseDTO responseDTO = new ResponseDTO();
      responseDTO.setQuestionnaireId(TEST_QID);
      responseDTO.setResponseDateTime(OffsetDateTime.now());
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setResponse(responseDTO);
      ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
      responseManagementEvent.setPayload(payloadDTO);

      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.RESPONSE_RECEIVED);
      eventDTO.setSource("RH");
      eventDTO.setDateTime(OffsetDateTime.now());
      eventDTO.setChannel("RH");
      eventDTO.setTransactionId(UUID.randomUUID());
      responseManagementEvent.setEvent(eventDTO);

      rabbitQueueHelper.sendMessage(inboundReceiptQueue, responseManagementEvent);

      //  THEN
      ResponseManagementEvent caseEmittedEvent = rhCaseQueueSpy.checkExpectedMessageReceived();

      CollectionCase emittedCase = caseEmittedEvent.getPayload().getCollectionCase();
      assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);
      assertThat(emittedCase.getSample()).isEqualTo(sample);
      assertThat(emittedCase.isReceiptReceived()).isTrue();

      ResponseManagementEvent uacUpdatedEvent = rhUacQueueSpy.checkExpectedMessageReceived();
      UacDTO emittedUac = uacUpdatedEvent.getPayload().getUac();
      assertThat(emittedUac.isActive()).isFalse();

      List<Event> storedEvents = eventRepository.findAll();
      assertThat(storedEvents.size()).isEqualTo(1);
      assertThat(storedEvents.get(0).getUacQidLink().getId()).isEqualTo(TEST_UACLINK_ID);
      assertThat(storedEvents.get(0).getEventType()).isEqualTo(EventType.RESPONSE_RECEIVED);
    }
  }
}
