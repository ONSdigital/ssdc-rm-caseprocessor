package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.NEW_CASE_TOPIC;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.NewCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class NewCaseReceiverIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private EventRepository eventRepository;
  @Autowired private CaseRepository caseRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testNewCaseLoaded() throws InterruptedException {
    try (QueueSpy<EventDTO> outboundCaseQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_CASE_SUBSCRIPTION, EventDTO.class)) {

      // GIVEN
      EventDTO event = new EventDTO();
      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
      eventHeader.setTopic(NEW_CASE_TOPIC);
      junkDataHelper.junkify(eventHeader);
      event.setHeader(eventHeader);

      CollectionExercise collectionExercise = junkDataHelper.setupJunkCollex();

      Map<String, String> sample = new HashMap<>();
      sample.put("Junk", "YesYouCan");
      sample.put("Org", "Brewery");

      Map<String, String> sampleSensitive = new HashMap<>();
      sampleSensitive.put("Telephone", "02071234567");

      PayloadDTO payloadDTO = new PayloadDTO();
      NewCase newCase = new NewCase();
      newCase.setCaseId(TEST_CASE_ID);
      newCase.setCollectionExerciseId(collectionExercise.getId());
      newCase.setSample(sample);
      newCase.setSampleSensitive(sampleSensitive);
      payloadDTO.setNewCase(newCase);
      event.setPayload(payloadDTO);

      pubsubHelper.sendMessageToSharedProject(NEW_CASE_TOPIC, event);

      //  THEN
      EventDTO actualEvent = outboundCaseQueueSpy.checkExpectedMessageReceived();

      CaseUpdateDTO emittedCase = actualEvent.getPayload().getCaseUpdate();
      Assertions.assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);

      Case actualCase = caseRepository.findById(TEST_CASE_ID).get();

      assertThat(actualCase.getId()).isEqualTo(TEST_CASE_ID);
      assertThat(actualCase.getCollectionExercise().getId()).isEqualTo(collectionExercise.getId());
      assertThat(actualCase.getSample()).isEqualTo(sample);
      assertThat(actualCase.getSampleSensitive()).isEqualTo(sampleSensitive);

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isEqualTo(1);
      assertThat(events.get(0).getType()).isEqualTo(EventType.NEW_CASE);
      assertThat(events.get(0).getPayload()).contains("{\"Telephone\": \"REDACTED\"}");
    }
  }
}
