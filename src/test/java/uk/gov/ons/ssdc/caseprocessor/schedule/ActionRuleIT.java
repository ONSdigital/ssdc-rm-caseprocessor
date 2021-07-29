package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class ActionRuleIT {
  private static final String OUTBOUND_PRINTER_SUBSCRIPTION =
      "caseProcessor.printFileSvc.printBatchRow.subscription";

  private static final String PACK_CODE = "test-pack-code";
  private static final String PRINT_SUPPLIER = "test-print-supplier";

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Value("${queueconfig.print-topic}")
  private String printTopic;

  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private PrintTemplateRepository printTemplateRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_PRINTER_SUBSCRIPTION, printTopic);
    pubsubHelper.purgeMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testPrinterRule() throws Exception {
    try (QueueSpy<PrintRow> printerQueue =
            pubsubHelper.listen(OUTBOUND_PRINTER_SUBSCRIPTION, PrintRow.class);
        QueueSpy<ResponseManagementEvent> outboundUacQueue =
            pubsubHelper.listen(OUTBOUND_UAC_SUBSCRIPTION, ResponseManagementEvent.class)) {
      // Given
      CollectionExercise collectionExercise = setUpCollectionExercise();
      Case caze = setUpCase(collectionExercise);
      PrintTemplate printTemplate = setUpPrintTemplate();

      // When
      setUpActionRule(ActionRuleType.PRINT, collectionExercise, printTemplate);
      PrintRow printRow = printerQueue.getQueue().poll(20, TimeUnit.SECONDS);
      ResponseManagementEvent rme = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(printRow).isNotNull();
      assertThat(printRow.getBatchQuantity()).isEqualTo(1);
      assertThat(printRow.getPackCode()).isEqualTo(PACK_CODE);
      assertThat(printRow.getPrintSupplier()).isEqualTo(PRINT_SUPPLIER);
      assertThat(printRow.getRow()).startsWith("\"123\"|\"bar\"|\"");

      assertThat(rme).isNotNull();
      assertThat(rme.getEvent().getType()).isEqualTo(EventTypeDTO.UAC_UPDATED);
      assertThat(rme.getPayload().getUac().getCaseId()).isEqualTo(caze.getId());
    }
  }

  @Test
  public void testDeactivateUacRule() throws Exception {
    try (QueueSpy<ResponseManagementEvent> outboundUacQueue =
        pubsubHelper.listen(OUTBOUND_UAC_SUBSCRIPTION, ResponseManagementEvent.class)) {
      // Given
      CollectionExercise collectionExercise = setUpCollectionExercise();
      Case caze = setUpCase(collectionExercise);
      UacQidLink uacQidLink = setupUacQidLink(caze);

      // When
      setUpActionRule(ActionRuleType.DEACTIVATE_UAC, collectionExercise, null);
      ResponseManagementEvent rme = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(rme).isNotNull();
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
    Case randomCase = new Case();
    randomCase.setId(UUID.randomUUID());
    randomCase.setCaseRef(123L);
    randomCase.setCollectionExercise(collectionExercise);
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
