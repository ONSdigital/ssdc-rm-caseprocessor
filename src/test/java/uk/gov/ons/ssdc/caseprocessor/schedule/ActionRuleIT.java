package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;

import java.time.OffsetDateTime;
import java.util.List;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.PrintFileRow;
import uk.gov.ons.ssdc.common.model.entity.PrintTemplate;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ActionRuleIT {
  private static final String OUTBOUND_PRINTER_SUBSCRIPTION =
      "rm-internal-print-row_print-file-service";

  private static final String PACK_CODE = "test-pack-code";
  private static final String PRINT_SUPPLIER = "test-print-supplier";

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private PrintTemplateRepository printTemplateRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;
  @Autowired private PrintFileRowRepository printFileRowRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  void testPrinterRule() throws Exception {
    try (QueueSpy<EventDTO> outboundUacQueue =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      // Given
      Case caze = junkDataHelper.setupJunkCase();
      PrintTemplate printTemplate = setUpPrintTemplate();

      // When
      setUpActionRule(ActionRuleType.PRINT, caze.getCollectionExercise(), printTemplate);
      EventDTO rme = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);
      List<PrintFileRow> printFileRows = printFileRowRepository.findAll();
      PrintFileRow printFileRow = printFileRows.get(0);

      // Then
      assertThat(printFileRow).isNotNull();
      assertThat(printFileRow.getBatchQuantity()).isEqualTo(1);
      assertThat(printFileRow.getPackCode()).isEqualTo(PACK_CODE);
      assertThat(printFileRow.getPrintSupplier()).isEqualTo(PRINT_SUPPLIER);
      assertThat(printFileRow.getRow()).startsWith("\"" + caze.getCaseRef() + "\"|\"bar\"|\"");

      assertThat(rme).isNotNull();
      assertThat(rme.getHeader().getTopic()).isEqualTo(uacUpdateTopic);
      assertThat(rme.getPayload().getUacUpdate().getCaseId()).isEqualTo(caze.getId());
    }
  }

  @Test
  void testDeactivateUacRule() throws Exception {
    try (QueueSpy<EventDTO> outboundUacQueue =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      // Given
      Case caze = junkDataHelper.setupJunkCase();
      UacQidLink uacQidLink = setupUacQidLink(caze);

      // When
      setUpActionRule(ActionRuleType.DEACTIVATE_UAC, caze.getCollectionExercise(), null);
      EventDTO rme = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(rme).isNotNull();
      assertThat(rme.getHeader().getTopic()).isEqualTo(uacUpdateTopic);
      assertThat(rme.getPayload().getUacUpdate().getCaseId()).isEqualTo(caze.getId());
      assertThat(rme.getPayload().getUacUpdate().isActive()).isFalse();
      assertThat(rme.getPayload().getUacUpdate().getQid()).isEqualTo(uacQidLink.getQid());

      assertThat(uacQidLinkRepository.findByQid(uacQidLink.getQid()).get().isActive()).isFalse();
    }
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

  private UacQidLink setupUacQidLink(Case caze) {
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid("123456789");
    uacQidLink.setUac("abc");
    uacQidLink.setActive(true);
    uacQidLink.setCaze(caze);
    return uacQidLinkRepository.saveAndFlush(uacQidLink);
  }
}
