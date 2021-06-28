package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_QUEUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jeasy.random.EasyRandom;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.*;
import uk.gov.ons.ssdc.caseprocessor.model.entity.*;
import uk.gov.ons.ssdc.caseprocessor.model.repository.*;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class FulfilmentIT {
  @Value("${queueconfig.print-queue}")
  private String outboundPrinterQueue;

  @Value("${queueconfig.fulfilment-queue}")
  private String outboundFulfilmentQueue;

  private static final String PACK_CODE = "TEST_FULFILMENT_CODE";
  private static final String PRINT_SUPPLIER = "FOOBAR_PRINT_SUPPLIER";

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;
  @Autowired private FulfilmentToProcessRepository fulfilmentToProcessRepository;
  @Autowired private FulfilmentTemplateRepository fulfilmentTemplateRepository;
  @Autowired private WaveOfContactRepository waveOfContactRepository;
  @Autowired private CaseToProcessRepository caseToProcessRepository;

  private static final EasyRandom easyRandom = new EasyRandom();
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(outboundPrinterQueue);
    rabbitQueueHelper.purgeQueue(OUTBOUND_UAC_QUEUE);
    waveOfContactRepository.deleteAllInBatch();
    caseToProcessRepository.deleteAllInBatch();
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
    fulfilmentNextTriggerRepository.deleteAllInBatch();
    fulfilmentToProcessRepository.deleteAllInBatch();
  }

  @Test
  public void testFulfilmentTrigger() throws Exception {
    try (QueueSpy printerQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
        QueueSpy outboundUacQueue = rabbitQueueHelper.listen(OUTBOUND_UAC_QUEUE)) {
      // Given
      FulfilmentTemplate fulfilmentTemplate = new FulfilmentTemplate();
      fulfilmentTemplate.setFulfilmentCode(PACK_CODE);
      fulfilmentTemplate.setPrintSupplier(PRINT_SUPPLIER);
      fulfilmentTemplate.setTemplate(new String[] {"__caseref__", "foo", "__uac__"});
      fulfilmentTemplateRepository.saveAndFlush(fulfilmentTemplate);

      CollectionExercise collectionExercise = setUpCollectionExercise();
      Case caze = setUpCase(collectionExercise);

      // When
      FulfilmentDTO fulfilment = new FulfilmentDTO();
      fulfilment.setCaseId(caze.getId());
      fulfilment.setFulfilmentCode(PACK_CODE);

      PayloadDTO payload = new PayloadDTO();
      payload.setFulfilment(fulfilment);

      ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
      responseManagementEvent.setPayload(payload);

      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.FULFILMENT);
      eventDTO.setSource("RH");
      eventDTO.setDateTime(OffsetDateTime.now());
      eventDTO.setChannel("RH");
      eventDTO.setTransactionId(UUID.randomUUID());
      responseManagementEvent.setEvent(eventDTO);

      rabbitQueueHelper.sendMessage(outboundFulfilmentQueue, responseManagementEvent);

      Thread.sleep(3000);

      FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
      fulfilmentNextTrigger.setId(UUID.randomUUID());
      fulfilmentNextTrigger.setTriggerDateTime(OffsetDateTime.now());
      fulfilmentNextTriggerRepository.saveAndFlush(fulfilmentNextTrigger);

      String printRowMessage = printerQueue.getQueue().poll(20, TimeUnit.SECONDS);
      String uacMessage = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(printRowMessage).isNotNull();
      PrintRow printRow = objectMapper.readValue(printRowMessage, PrintRow.class);

      assertThat(printRow.getBatchQuantity()).isEqualTo(1);
      assertThat(printRow.getPackCode()).isEqualTo(PACK_CODE);
      assertThat(printRow.getPrintSupplier()).isEqualTo(PRINT_SUPPLIER);
      assertThat(printRow.getRow()).startsWith("\"123\"|\"bar\"|\"");

      assertThat(uacMessage).isNotNull();
      ResponseManagementEvent rme =
          objectMapper.readValue(uacMessage, ResponseManagementEvent.class);
      assertThat(rme.getEvent().getType()).isEqualTo(EventTypeDTO.UAC_UPDATED);
      assertThat(rme.getPayload().getUac().getCaseId()).isEqualTo(caze.getId());
    }
  }

  private CollectionExercise setUpCollectionExercise() {
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    return collectionExerciseRepository.saveAndFlush(collectionExercise);
  }

  private Case setUpCase(CollectionExercise collectionExercise) {
    Case randomCase = easyRandom.nextObject(Case.class);
    randomCase.setCaseRef(123L);
    randomCase.setCollectionExercise(collectionExercise);
    randomCase.setUacQidLinks(null);
    randomCase.setReceiptReceived(false);
    randomCase.setRefusalReceived(null);
    randomCase.setAddressInvalid(false);
    randomCase.setCreatedAt(null);
    randomCase.setLastUpdatedAt(null);
    randomCase.setEvents(null);
    randomCase.setSample(Map.of("foo", "bar"));
    return caseRepository.saveAndFlush(randomCase);
  }
}