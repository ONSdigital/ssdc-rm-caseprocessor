package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;

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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
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
  private static final String UPDATE_SAMPLE_SENSITIVE_TOPIC =
      "events.caseProcessor.updateSampleSensitive.topic";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private EventPoller eventPoller;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
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

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payloadDTO);

    EventDTO eventDTO = new EventDTO();
    eventDTO.setType(EventTypeDTO.UPDATE_SAMPLE_SENSITIVE);
    eventDTO.setSource("RH");
    eventDTO.setDateTime(OffsetDateTime.now());
    eventDTO.setChannel("RH");
    eventDTO.setTransactionId(UUID.randomUUID());
    responseManagementEvent.setEvent(eventDTO);

    //  When
    pubsubHelper.sendMessage(UPDATE_SAMPLE_SENSITIVE_TOPIC, responseManagementEvent);

    List<Event> events = eventPoller.getEvents(1);

    //  Then
    Case actualCase = caseRepository.findById(TEST_CASE_ID).get();
    assertThat(actualCase.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", "9999999"));

    assertThat(events.size()).isEqualTo(1);
    Event event = events.get(0);
    assertThat(event.getCaze().getId()).isEqualTo(TEST_CASE_ID);
    assertThat(event.getEventType()).isEqualTo(EventType.UPDATE_SAMPLE_SENSITIVE);

    PayloadDTO actualPayload = objectMapper.readValue(event.getEventPayload(), PayloadDTO.class);
    assertThat(actualPayload.getUpdateSampleSensitive().getSampleSensitive())
        .isEqualTo(Map.of("PHONE_NUMBER", "REDACTED"));
  }
}
