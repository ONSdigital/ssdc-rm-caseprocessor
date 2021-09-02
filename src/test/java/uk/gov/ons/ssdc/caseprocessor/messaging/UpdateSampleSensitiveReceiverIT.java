package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.HashMap;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSampleSensitive;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.EventPoller;
import uk.gov.ons.ssdc.caseprocessor.testutils.EventsNotFoundException;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UpdateSampleSensitiveReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();
  private static final String UPDATE_SAMPLE_SENSITIVE_TOPIC = "event_update-sample-sensitive";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private EventPoller eventPoller;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testUpdateSampleSensitive() throws JsonProcessingException, EventsNotFoundException {
    // GIVEN

    Case caze = new Case();
    caze.setId(TEST_CASE_ID);
    Map<String, String> sensitiveData = new HashMap<>();
    sensitiveData.put("PHONE_NUMBER", "1111111");
    caze.setSampleSensitive(sensitiveData);

    caseRepository.saveAndFlush(caze);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUpdateSampleSensitive(new UpdateSampleSensitive());
    payloadDTO.getUpdateSampleSensitive().setCaseId(TEST_CASE_ID);
    payloadDTO.getUpdateSampleSensitive().setSampleSensitive(Map.of("PHONE_NUMBER", "9999999"));

    EventDTO event = new EventDTO();
    event.setPayload(payloadDTO);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    eventHeader.setTopic(UPDATE_SAMPLE_SENSITIVE_TOPIC);
    eventHeader.setSource("RH");
    eventHeader.setDateTime(OffsetDateTime.now());
    eventHeader.setChannel("RH");
    eventHeader.setMessageId(UUID.randomUUID());
    event.setHeader(eventHeader);

    //  When
    pubsubHelper.sendMessageToSharedProject(UPDATE_SAMPLE_SENSITIVE_TOPIC, event);

    List<Event> databaseEvents = eventPoller.getEvents(1);

    //  Then
    Case actualCase = caseRepository.findById(TEST_CASE_ID).get();
    assertThat(actualCase.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", "9999999"));

    assertThat(databaseEvents.size()).isEqualTo(1);
    Event databaseEvent = databaseEvents.get(0);
    assertThat(databaseEvent.getCaze().getId()).isEqualTo(TEST_CASE_ID);
    assertThat(databaseEvent.getType()).isEqualTo(EventType.UPDATE_SAMPLE_SENSITIVE);

    PayloadDTO actualPayload = objectMapper.readValue(databaseEvent.getPayload(), PayloadDTO.class);
    assertThat(actualPayload.getUpdateSampleSensitive().getSampleSensitive())
        .isEqualTo(Map.of("PHONE_NUMBER", "REDACTED"));
  }
}
