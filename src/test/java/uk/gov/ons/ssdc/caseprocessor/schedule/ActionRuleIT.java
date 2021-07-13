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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRule;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.PrintTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class ActionRuleIT {
  @Value("${queueconfig.print-queue}")
  private String outboundPrinterQueue;

  private static final String PACK_CODE = "test-pack-code";
  private static final String PRINT_SUPPLIER = "test-print-supplier";

  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private PrintTemplateRepository printTemplateRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;

  private static final EasyRandom easyRandom = new EasyRandom();
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @Before
  public void setUp() {
    rabbitQueueHelper.purgeQueue(outboundPrinterQueue);
    rabbitQueueHelper.purgeQueue(OUTBOUND_UAC_QUEUE);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testPrinterRule() throws Exception {
    try (QueueSpy printerQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
        QueueSpy outboundUacQueue = rabbitQueueHelper.listen(OUTBOUND_UAC_QUEUE)) {
      // Given
      CollectionExercise collectionExercise = setUpCollectionExercise();
      Case caze = setUpCase(collectionExercise);
      PrintTemplate printTemplate = setUpPrintTemplate();

      // When
      setUpActionRule(ActionRuleType.PRINT, collectionExercise, printTemplate);
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

  @Test
  public void testDeactivateUacRule() throws Exception {
    try (QueueSpy outboundUacQueue = rabbitQueueHelper.listen(OUTBOUND_UAC_QUEUE)) {
      // Given
      CollectionExercise collectionExercise = setUpCollectionExercise();
      Case caze = setUpCase(collectionExercise);
      UacQidLink uacQidLink = setupUacQidLink(caze);

      // When
      setUpActionRule(ActionRuleType.DEACTIVATE_UAC, collectionExercise, null);
      String uacMessage = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(uacMessage).isNotNull();
      ResponseManagementEvent rme =
          objectMapper.readValue(uacMessage, ResponseManagementEvent.class);
      assertThat(rme.getEvent().getType()).isEqualTo(EventTypeDTO.UAC_UPDATED);
      assertThat(rme.getPayload().getUac().getCaseId()).isEqualTo(caze.getId());
      assertThat(rme.getPayload().getUac().isActive()).isFalse();
      assertThat(rme.getPayload().getUac().getQuestionnaireId()).isEqualTo(uacQidLink.getQid());

      assertThat(uacQidLinkRepository.findByQid(uacQidLink.getQid()).get().isActive()).isFalse();
    }
  }

  private CollectionExercise setUpCollectionExercise() {
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    return collectionExerciseRepository.saveAndFlush(collectionExercise);
  }

  private PrintTemplate setUpPrintTemplate() {
    PrintTemplate printTemplate = new PrintTemplate();
    printTemplate.setTemplate(new String[] {"__caseref__", "foo", "__uac__"});
    printTemplate.setPackCode(PACK_CODE);
    printTemplate.setPrintSupplier(PRINT_SUPPLIER);
    return printTemplateRepository.saveAndFlush(printTemplate);
  }

  private ActionRule setUpActionRule(
      ActionRuleType type, CollectionExercise collectionExercise, PrintTemplate printTemplate) {
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);
    actionRule.setType(type);
    actionRule.setCollectionExercise(collectionExercise);
    actionRule.setPrintTemplate(printTemplate);

    return actionRuleRepository.saveAndFlush(actionRule);
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

  private UacQidLink setupUacQidLink(Case caze) {
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid("123456789");
    uacQidLink.setActive(true);
    uacQidLink.setCaze(caze);
    return uacQidLinkRepository.saveAndFlush(uacQidLink);
  }
}
