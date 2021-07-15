package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_QUEUE;

import java.time.OffsetDateTime;
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
import uk.gov.ons.ssdc.caseprocessor.client.UacQidServiceClient;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.TelephoneCaptureDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TelephoneCaptureReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();

  @Value("${queueconfig.telephone-capture-queue}")
  private String telephoneCaptureQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private UacQidServiceClient uacQidServiceClient;

  //  private static final EasyRandom easyRandom = new EasyRandom();

  @Before
  public void setUp() {
    rabbitQueueHelper.purgeQueue(telephoneCaptureQueue);
    rabbitQueueHelper.purgeQueue(OUTBOUND_UAC_QUEUE);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testTelephoneCapture() throws Exception {
    // Given
    // Get a new UAC QID pair
    List<UacQidDTO> uacQidDTOList = uacQidServiceClient.getUacQids(1, 1);
    UacQidDTO telephoneCaptureUacQid = uacQidDTOList.get(0);

    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    collectionExerciseRepository.saveAndFlush(collectionExercise);

    // Create the case
    Case testCase = setUpCase(collectionExercise);

    // Build the event message
    TelephoneCaptureDTO telephoneCaptureDTO = new TelephoneCaptureDTO();
    telephoneCaptureDTO.setUac(telephoneCaptureUacQid.getUac());
    telephoneCaptureDTO.setQid(telephoneCaptureUacQid.getQid());
    telephoneCaptureDTO.setCaseId(testCase.getId());
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setTelephoneCapture(telephoneCaptureDTO);
    EventDTO eventDTO = new EventDTO();
    eventDTO.setDateTime(OffsetDateTime.now());
    eventDTO.setType(EventTypeDTO.TELEPHONE_CAPTURE_REQUESTED);
    eventDTO.setTransactionId(UUID.randomUUID());
    eventDTO.setChannel("RM");
    eventDTO.setSource("RM");
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(eventDTO);
    responseManagementEvent.setPayload(payloadDTO);

    try (QueueSpy outboundUacQueueSpy = rabbitQueueHelper.listen(OUTBOUND_UAC_QUEUE)) {
      rabbitQueueHelper.sendMessage(telephoneCaptureQueue, responseManagementEvent);
      ResponseManagementEvent emittedEvent = outboundUacQueueSpy.checkExpectedMessageReceived();

      assertThat(emittedEvent.getEvent().getType()).isEqualTo(EventTypeDTO.UAC_UPDATED);

      UacDTO uacUpdatedEvent = emittedEvent.getPayload().getUac();
      assertThat(uacUpdatedEvent.getCaseId()).isEqualTo(testCase.getId());
      assertThat(uacUpdatedEvent.getUac()).isEqualTo(telephoneCaptureUacQid.getUac());
      assertThat(uacUpdatedEvent.getQuestionnaireId()).isEqualTo(telephoneCaptureUacQid.getQid());
      assertThat(uacUpdatedEvent.getCollectionExerciseId()).isEqualTo(collectionExercise.getId());
    }
  }

  private Case setUpCase(CollectionExercise collectionExercise) {
    Case randomCase = new Case();
    randomCase.setId(TEST_CASE_ID);
    randomCase.setCollectionExercise(collectionExercise);
    randomCase.setCaseRef(123L);
    randomCase.setSample(Map.of("foo", "bar"));
    randomCase.setUacQidLinks(null);
    randomCase.setEvents(null);
    return caseRepository.saveAndFlush(randomCase);
  }
}
