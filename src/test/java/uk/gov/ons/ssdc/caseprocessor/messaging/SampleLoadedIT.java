package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.WaveOfContactRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.RabbitQueueHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.TestHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SampleLoadedIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();

  @Value("${queueconfig.sample-queue}")
  private String inboundQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private TestHelper testHelper;
  @Autowired private CaseRepository caseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private WaveOfContactRepository waveOfContactRepository;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundQueue);
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    waveOfContactRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
  }

  @Test
  public void testSampleLoaded() throws IOException, InterruptedException {
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    collectionExerciseRepository.saveAndFlush(collectionExercise);

    Map<String, String> sampleDto = new HashMap<>();
    sampleDto.put("Address", "Tenby");
    sampleDto.put("Org", "Brewery");

    Sample sample = new Sample();
    sample.setCaseId(TEST_CASE_ID);
    sample.setCollectionExerciseId(collectionExercise.getId());
    sample.setSample(sampleDto);

    rabbitQueueHelper.sendMessage(inboundQueue, sample);

    Case actualCase = testHelper.pollUntilCaseExists(TEST_CASE_ID);

    assertThat(actualCase.getId()).isEqualTo(TEST_CASE_ID);
    assertThat(actualCase.getCollectionExercise().getId()).isEqualTo(collectionExercise.getId());
    assertThat(actualCase.getSample()).isEqualTo(sampleDto);

    List<Event> events = eventRepository.findAll();
    assertThat(events.size()).isEqualTo(1);
    assertThat(events.get(0).getEventType()).isEqualTo(EventType.SAMPLE_LOADED);
  }
}
