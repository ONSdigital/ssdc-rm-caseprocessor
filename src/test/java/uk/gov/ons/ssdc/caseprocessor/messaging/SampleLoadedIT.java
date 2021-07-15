package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_QUEUE;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SampleLoadedIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();

  @Value("${queueconfig.sample-queue}")
  private String inboundQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseService caseService;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private EventRepository eventRepository;

  @Before
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundQueue);
    rabbitQueueHelper.purgeQueue(OUTBOUND_CASE_QUEUE);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testSampleLoaded() throws IOException, InterruptedException {
    try (QueueSpy outboundCaseQueueSpy = rabbitQueueHelper.listen(OUTBOUND_CASE_QUEUE)) {
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

      rabbitQueueHelper.sendMessage(inboundQueue, sampleDto);

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
      assertThat(events.get(0).getEventType()).isEqualTo(EventType.SAMPLE_LOADED);
    }
  }
}
