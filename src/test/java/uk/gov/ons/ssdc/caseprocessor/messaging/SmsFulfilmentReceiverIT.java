package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.SMS_FULFILMENT_TOPIC;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
import uk.gov.ons.ssdc.caseprocessor.client.UacQidServiceClient;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EnrichedSmsFulfilment;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.utils.HashHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
class SmsFulfilmentReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String PACK_CODE = "TEST_SMS";

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private UacQidServiceClient uacQidServiceClient;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  void testSmsFulfilment() throws Exception {
    // Given
    // Get a new UAC QID pair
    List<UacQidDTO> uacQidDTOList = uacQidServiceClient.getUacQids(1, 1);
    UacQidDTO smsUacQid = uacQidDTOList.get(0);

    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    collectionExerciseRepository.saveAndFlush(collectionExercise);

    // Create the case
    Case testCase = setUpCase(collectionExercise);

    // Build the event message
    EnrichedSmsFulfilment enrichedSmsFulfilment = new EnrichedSmsFulfilment();
    enrichedSmsFulfilment.setUac(smsUacQid.getUac());
    enrichedSmsFulfilment.setQid(smsUacQid.getQid());
    enrichedSmsFulfilment.setCaseId(testCase.getId());
    enrichedSmsFulfilment.setPackCode(PACK_CODE);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setEnrichedSmsFulfilment(enrichedSmsFulfilment);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    eventHeader.setDateTime(OffsetDateTime.now());
    eventHeader.setTopic(SMS_FULFILMENT_TOPIC);
    eventHeader.setMessageId(UUID.randomUUID());
    eventHeader.setChannel("RM");
    eventHeader.setSource("RM");

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    try (QueueSpy<EventDTO> outboundUacQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      pubsubHelper.sendMessage(SMS_FULFILMENT_TOPIC, event);
      EventDTO emittedEvent = outboundUacQueueSpy.checkExpectedMessageReceived();

      assertThat(emittedEvent.getHeader().getTopic()).isEqualTo(uacUpdateTopic);

      UacUpdateDTO uacUpdatedEvent = emittedEvent.getPayload().getUacUpdate();
      assertThat(uacUpdatedEvent.getCaseId()).isEqualTo(testCase.getId());
      assertThat(uacUpdatedEvent.getUacHash()).isEqualTo(HashHelper.hash(smsUacQid.getUac()));
      assertThat(uacUpdatedEvent.getQid()).isEqualTo(smsUacQid.getQid());
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
