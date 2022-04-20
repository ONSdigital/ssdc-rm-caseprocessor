package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSample;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.EventPoller;
import uk.gov.ons.ssdc.caseprocessor.testutils.EventsNotFoundException;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;
import uk.gov.ons.ssdc.common.validation.MandatoryRule;
import uk.gov.ons.ssdc.common.validation.Rule;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UpdateSampleReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final Long TEST_CASE_REF = 1234567890L;
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();
  private static final String UPDATE_SAMPLE_TOPIC = "event_update-sample";

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private EventPoller eventPoller;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_CASE_SUBSCRIPTION, UPDATE_SAMPLE_TOPIC);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testUpdateSample() throws JsonProcessingException, EventsNotFoundException {
    // GIVEN

    Case caze = new Case();
    caze.setId(TEST_CASE_ID);
    caze.setCaseRef(TEST_CASE_REF);
    Map<String, String> sampleData = new HashMap<>();
    sampleData.put("testSampleField", "Test");
    caze.setSample(sampleData);
    caze.setSampleSensitive(new HashMap<>());

    caze.setCollectionExercise(
        junkDataHelper.setUpJunkCollexWithTheseColumnValidators(
            new ColumnValidator[] {
              new ColumnValidator("testSampleField", false, new Rule[] {new MandatoryRule()}),
            }));
    caseRepository.saveAndFlush(caze);

    EventDTO event = prepareEvent("testSampleField");

    //  When
    pubsubHelper.sendMessageToSharedProject(UPDATE_SAMPLE_TOPIC, event);

    List<Event> databaseEvents = eventPoller.getEvents(1);

    //  Then
    Case actualCase = caseRepository.findById(TEST_CASE_ID).get();
    assertThat(actualCase.getSample()).isEqualTo(Map.of("testSampleField", "Updated"));

    assertThat(databaseEvents.size()).isEqualTo(1);
    Event databaseEvent = databaseEvents.get(0);
    assertThat(databaseEvent.getCaze().getId()).isEqualTo(TEST_CASE_ID);
    assertThat(databaseEvent.getType()).isEqualTo(EventType.UPDATE_SAMPLE);

    PayloadDTO actualPayload = objectMapper.readValue(databaseEvent.getPayload(), PayloadDTO.class);
    assertThat(actualPayload.getUpdateSample().getSample())
        .isEqualTo(Map.of("testSampleField", "Updated"));
  }

  @Test
  public void testUpdateSampleSimultaneousRequestsOnSameCase() throws EventsNotFoundException {
    // Given
    Case caze = new Case();
    caze.setId(TEST_CASE_ID);
    caze.setCaseRef(TEST_CASE_REF);
    Map<String, String> sampleData = new HashMap<>();
    sampleData.put("testSampleFieldA", "Original value");
    sampleData.put("testSampleFieldB", "Original value");
    sampleData.put("testSampleFieldC", "Original value");
    sampleData.put("testSampleFieldD", "Original value");
    caze.setSample(sampleData);
    caze.setSampleSensitive(new HashMap<>());

    CollectionExercise collectionExercise =
        junkDataHelper.setUpJunkCollexWithTheseColumnValidators(
            new ColumnValidator[] {
              new ColumnValidator("testSampleFieldA", false, new Rule[] {new MandatoryRule()}),
              new ColumnValidator("testSampleFieldB", false, new Rule[] {new MandatoryRule()}),
              new ColumnValidator("testSampleFieldC", false, new Rule[] {new MandatoryRule()}),
              new ColumnValidator("testSampleFieldD", false, new Rule[] {new MandatoryRule()})
            });

    caze.setCollectionExercise(collectionExercise);
    caseRepository.saveAndFlush(caze);

    EventDTO[] events =
        new EventDTO[] {
          prepareEvent("testSampleFieldA"),
          prepareEvent("testSampleFieldB"),
          prepareEvent("testSampleFieldC"),
          prepareEvent("testSampleFieldD")
        };

    // When
    Arrays.stream(events)
        .parallel()
        .forEach(
            event -> {
              pubsubHelper.sendMessageToSharedProject(UPDATE_SAMPLE_TOPIC, event);
            });

    eventPoller.getEvents(4);

    // Then
    Case actualCase = caseRepository.findById(TEST_CASE_ID).get();
    assertThat(actualCase.getSample())
        .isEqualTo(
            Map.of(
                "testSampleFieldA",
                "Updated",
                "testSampleFieldB",
                "Updated",
                "testSampleFieldC",
                "Updated",
                "testSampleFieldD",
                "Updated"));
  }

  private EventDTO prepareEvent(String sampleFieldToUpdate) {
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUpdateSample(new UpdateSample());
    payloadDTO.getUpdateSample().setCaseId(TEST_CASE_ID);
    payloadDTO.getUpdateSample().setSample(Map.of(sampleFieldToUpdate, "Updated"));

    EventDTO event = new EventDTO();
    event.setPayload(payloadDTO);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    eventHeader.setTopic(UPDATE_SAMPLE_TOPIC);
    junkDataHelper.junkify(eventHeader);
    event.setHeader(eventHeader);
    return event;
  }
}
