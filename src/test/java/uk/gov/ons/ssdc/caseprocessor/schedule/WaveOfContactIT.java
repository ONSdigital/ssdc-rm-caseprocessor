package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.WaveOfContactRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class WaveOfContactIT {
  private static final String OUTBOUND_PRINTER_QUEUE = "Action.Printer";
  private static final String UAC_QUEUE = "case.rh.uac";
  private static final String PACK_CODE = "test-pack-code";
  private static final String PRINT_SUPPLIER = "test-print-supplier";

  @Autowired private CaseRepository caseRepository;
  @Autowired private WaveOfContactRepository waveOfContactRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private CaseToProcessRepository caseToProcessRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  private static final EasyRandom easyRandom = new EasyRandom();
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(OUTBOUND_PRINTER_QUEUE);
    rabbitQueueHelper.purgeQueue(UAC_QUEUE);
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseToProcessRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    waveOfContactRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
  }

  @Test
  public void testPrinterRule() throws Exception {
    try (QueueSpy printerQueue = rabbitQueueHelper.listen(OUTBOUND_PRINTER_QUEUE);
        QueueSpy uacQueue = rabbitQueueHelper.listen(UAC_QUEUE)) {
      // Given
      CollectionExercise collectionExercise = setUpCollectionExercise();
      Case caze = setUpCase(collectionExercise);

      // When
      setUpWaveOfContact(WaveOfContactType.PRINT, collectionExercise);
      String printRowMessage = printerQueue.getQueue().poll(20, TimeUnit.SECONDS);
      String uacMessage = uacQueue.getQueue().poll(20, TimeUnit.SECONDS);

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

  private WaveOfContact setUpWaveOfContact(
      WaveOfContactType type, CollectionExercise collectionExercise) {
    WaveOfContact waveOfContact = new WaveOfContact();
    waveOfContact.setId(UUID.randomUUID());
    waveOfContact.setTriggerDateTime(OffsetDateTime.now());
    waveOfContact.setHasTriggered(false);
    waveOfContact.setType(type);
    waveOfContact.setCollectionExercise(collectionExercise);
    waveOfContact.setClassifiers("1=1"); // Dummy classifier which is always true
    waveOfContact.setTemplate(new String[] {"__caseref__", "foo", "__uac__"});
    waveOfContact.setPackCode(PACK_CODE);
    waveOfContact.setPrintSupplier(PRINT_SUPPLIER);

    return waveOfContactRepository.saveAndFlush(waveOfContact);
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
    randomCase.setSample(Map.of("foo", "bar"));
    return caseRepository.saveAndFlush(randomCase);
  }
}
