package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;

import java.io.IOException;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.CollectionCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SampleLoadedIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String SAMPLE_TOPIC = "rm-internal-sample";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseService caseService;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private EventRepository eventRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testSampleLoaded() throws IOException, InterruptedException {
    try (QueueSpy<ResponseManagementEvent> outboundCaseQueueSpy =
        pubsubHelper.listen(OUTBOUND_CASE_SUBSCRIPTION, ResponseManagementEvent.class)) {
      CollectionExercise collectionExercise = new CollectionExercise();
      collectionExercise.setId(UUID.randomUUID());
      collectionExerciseRepository.saveAndFlush(collectionExercise);

      Map<String, String> sample = new HashMap<>();
      sample.put("Address", "Tenby");
      sample.put("Org", "Brewery");

      Map<String, String> sampleSensitive = new HashMap<>();
      sample.put("The Queen's Private Telephone Number", "02071234567");

      Sample sampleDto = new Sample();
      sampleDto.setCaseId(TEST_CASE_ID);
      sampleDto.setCollectionExerciseId(collectionExercise.getId());
      sampleDto.setSample(sample);
      sampleDto.setSampleSensitive(sampleSensitive);

      pubsubHelper.sendMessage(SAMPLE_TOPIC, sampleDto);

      //  THEN
      ResponseManagementEvent actualResponseManagementEvent =
          outboundCaseQueueSpy.checkExpectedMessageReceived();

      CollectionCase emittedCase = actualResponseManagementEvent.getPayload().getCollectionCase();
      Assertions.assertThat(emittedCase.getCaseId()).isEqualTo(TEST_CASE_ID);

      Case actualCase = caseService.getCaseByCaseId(TEST_CASE_ID);

      assertThat(actualCase.getId()).isEqualTo(TEST_CASE_ID);
      assertThat(actualCase.getCollectionExercise().getId()).isEqualTo(collectionExercise.getId());
      assertThat(actualCase.getSample()).isEqualTo(sample);
      assertThat(actualCase.getSampleSensitive()).isEqualTo(sampleSensitive);

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isEqualTo(1);
      assertThat(events.get(0).getEventType()).isEqualTo(EventType.CASE_CREATED);
    }
  }
}
