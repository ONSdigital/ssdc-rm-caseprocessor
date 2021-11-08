package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_EMAIL_REQUEST_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_SMS_REQUEST_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;

import java.time.OffsetDateTime;
import java.util.List;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EmailTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.SmsTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JsonHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.EmailTemplate;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.ExportFileRow;
import uk.gov.ons.ssdc.common.model.entity.ExportFileTemplate;
import uk.gov.ons.ssdc.common.model.entity.SmsTemplate;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ActionRuleIT {
  private static final String PACK_CODE = "test-pack-code";
  private static final String EXPORT_FILE_DESTINATION = "test-export-file-destination";
  private static final String CREATED_BY_USER = "test@ons.gov.uk";
  private static final Map<String, String> TEST_UAC_METADATA = Map.of("TEST_UAC_METADATA", "TEST");

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Value("${queueconfig.sms-request-topic}")
  private String smsRequestTopic;

  @Value("${queueconfig.email-request-topic}")
  private String emailRequestTopic;

  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private ExportFileTemplateRepository exportFileTemplateRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;
  @Autowired private ExportFileRowRepository exportFileRowRepository;
  @Autowired private SmsTemplateRepository smsTemplateRepository;
  @Autowired private EmailTemplateRepository emailTemplateRepository;
  @Autowired private EventRepository eventRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  void testExportFileRule() throws Exception {
    try (QueueSpy<EventDTO> outboundUacQueue =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      // Given
      Case caze = junkDataHelper.setupJunkCase();
      ExportFileTemplate exportFileTemplate = setUpExportFileTemplate();

      // When
      setUpActionRule(
          ActionRuleType.EXPORT_FILE, caze.getCollectionExercise(), exportFileTemplate, null, null);
      EventDTO rme = outboundUacQueue.getQueue().poll(20, TimeUnit.SECONDS);
      List<ExportFileRow> exportFileRows = exportFileRowRepository.findAll();
      ExportFileRow exportFileRow = exportFileRows.get(0);

      // Then
      assertThat(exportFileRow).isNotNull();
      assertThat(exportFileRow.getBatchQuantity()).isEqualTo(1);
      assertThat(exportFileRow.getPackCode()).isEqualTo(PACK_CODE);
      assertThat(exportFileRow.getExportFileDestination()).isEqualTo(EXPORT_FILE_DESTINATION);
      assertThat(exportFileRow.getRow()).startsWith("\"" + caze.getCaseRef() + "\"|\"bar\"|\"");

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
      setUpActionRule(
          ActionRuleType.DEACTIVATE_UAC, caze.getCollectionExercise(), null, null, null);
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

  @Test
  void testSmsRule() throws Exception {
    try (QueueSpy<EventDTO> smsRequestQueue =
        pubsubHelper.listen(OUTBOUND_SMS_REQUEST_SUBSCRIPTION, EventDTO.class)) {
      // Given
      Case caze = junkDataHelper.setupJunkCase();

      SmsTemplate smsTemplate = setupSmsTemplate();

      // When
      setUpActionRule(ActionRuleType.SMS, caze.getCollectionExercise(), null, smsTemplate, null);
      EventDTO rme = smsRequestQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(rme).isNotNull();
      assertThat(rme.getHeader().getTopic()).isEqualTo(smsRequestTopic);
      assertThat(rme.getPayload().getSmsRequest().getCaseId()).isEqualTo(caze.getId());
      assertThat(rme.getPayload().getSmsRequest().getPackCode()).isEqualTo("Test pack code");
      assertThat(rme.getPayload().getSmsRequest().getPhoneNumber()).isEqualTo("123");
      assertThat(rme.getPayload().getSmsRequest().getUacMetadata()).isEqualTo(TEST_UAC_METADATA);

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isOne();
      Event actualEvent = events.get(0);
      assertThat(actualEvent.getType()).isEqualTo(EventType.ACTION_RULE_SMS_REQUEST);
      PayloadDTO payloadDTO =
          JsonHelper.convertJsonBytesToObject(
              actualEvent.getPayload().getBytes(), PayloadDTO.class);
      assertThat(payloadDTO.getSmsRequest().getPhoneNumber()).isEqualTo("REDACTED");
    }
  }

  @Test
  void testEmailRule() throws Exception {
    try (QueueSpy<EventDTO> emailRequestQueue =
        pubsubHelper.listen(OUTBOUND_EMAIL_REQUEST_SUBSCRIPTION, EventDTO.class)) {
      // Given
      Case caze = junkDataHelper.setupJunkCase();

      EmailTemplate emailTemplate = setupEmailTemplate();

      // When
      setUpActionRule(
          ActionRuleType.EMAIL, caze.getCollectionExercise(), null, null, emailTemplate);
      EventDTO rme = emailRequestQueue.getQueue().poll(20, TimeUnit.SECONDS);

      // Then
      assertThat(rme).isNotNull();
      assertThat(rme.getHeader().getTopic()).isEqualTo(emailRequestTopic);
      assertThat(rme.getPayload().getEmailRequest().getCaseId()).isEqualTo(caze.getId());
      assertThat(rme.getPayload().getEmailRequest().getPackCode()).isEqualTo("Test pack code");
      assertThat(rme.getPayload().getEmailRequest().getEmail()).isEqualTo("junk@junk.com");
      assertThat(rme.getPayload().getEmailRequest().getUacMetadata()).isEqualTo(TEST_UAC_METADATA);

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isOne();
      Event actualEvent = events.get(0);
      assertThat(actualEvent.getType()).isEqualTo(EventType.ACTION_RULE_EMAIL_REQUEST);
      PayloadDTO payloadDTO =
          JsonHelper.convertJsonBytesToObject(
              actualEvent.getPayload().getBytes(), PayloadDTO.class);
      assertThat(payloadDTO.getEmailRequest().getEmail()).isEqualTo("REDACTED");
    }
  }

  private ExportFileTemplate setUpExportFileTemplate() {
    ExportFileTemplate exportFileTemplate = new ExportFileTemplate();
    exportFileTemplate.setTemplate(new String[] {"__caseref__", "foo", "__uac__"});
    exportFileTemplate.setPackCode(PACK_CODE);
    exportFileTemplate.setExportFileDestination(EXPORT_FILE_DESTINATION);
    exportFileTemplate.setDescription("Test description");
    return exportFileTemplateRepository.saveAndFlush(exportFileTemplate);
  }

  private ActionRule setUpActionRule(
      ActionRuleType type,
      CollectionExercise collectionExercise,
      ExportFileTemplate exportFileTemplate,
      SmsTemplate smsTemplate,
      EmailTemplate emailTemplate) {
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);
    actionRule.setType(type);
    actionRule.setCollectionExercise(collectionExercise);
    actionRule.setExportFileTemplate(exportFileTemplate);
    actionRule.setCreatedBy(CREATED_BY_USER);
    actionRule.setUacMetadata(TEST_UAC_METADATA);

    if (smsTemplate != null) {
      actionRule.setSmsTemplate(smsTemplate);
      actionRule.setPhoneNumberColumn("phoneNumber");
    }

    if (emailTemplate != null) {
      actionRule.setEmailTemplate(emailTemplate);
      actionRule.setEmailColumn("emailAddress");
    }

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

  private SmsTemplate setupSmsTemplate() {
    SmsTemplate smsTemplate = new SmsTemplate();
    smsTemplate.setPackCode("Test pack code");
    smsTemplate.setNotifyTemplateId(UUID.randomUUID());
    smsTemplate.setTemplate(new String[] {"FOO", "BAR"});
    smsTemplate.setDescription("Test description");
    return smsTemplateRepository.saveAndFlush(smsTemplate);
  }

  private EmailTemplate setupEmailTemplate() {
    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setPackCode("Test pack code");
    emailTemplate.setNotifyTemplateId(UUID.randomUUID());
    emailTemplate.setTemplate(new String[] {"FOO", "BAR"});
    emailTemplate.setDescription("Test description");
    return emailTemplateRepository.saveAndFlush(emailTemplate);
  }
}
