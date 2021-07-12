package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_QUEUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSampleSensitive;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.EventPoller;
import uk.gov.ons.ssdc.caseprocessor.testutils.EventsNotFoundException;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateSampleSensitiveReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @Value("${queueconfig.update-sample-sensitive-queue}")
  private String updateSampleSensitiveQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private CaseRepository caseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;
  @Autowired private EventPoller eventPoller;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(updateSampleSensitiveQueue);
    rabbitQueueHelper.purgeQueue(OUTBOUND_CASE_QUEUE);
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    actionRuleRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
  }

  @Test
  public void testUpdateSampleSensitive()
      throws JsonProcessingException, EventsNotFoundException {
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
    rabbitQueueHelper.sendMessage(updateSampleSensitiveQueue, responseManagementEvent);

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
        .isEqualTo(Map.of("REDACTED", "REDACTED"));
  }
}
