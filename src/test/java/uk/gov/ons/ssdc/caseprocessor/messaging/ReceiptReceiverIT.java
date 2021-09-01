package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ReceiptDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ReceiptReceiverIT {
  private static final String TEST_QID = "123456";
  private static final UUID TEST_UACLINK_ID = UUID.randomUUID();
  private static final String INBOUND_RECEIPT_TOPIC = "event_receipt";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private EventRepository eventRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testReceipt() throws Exception {
    try (QueueSpy<EventDTO> outboundUacQueueSpy =
            pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class);
        QueueSpy<EventDTO> outboundCaseQueueSpy =
            pubsubHelper.sharedProjectListen(OUTBOUND_CASE_SUBSCRIPTION, EventDTO.class)) {
      // GIVEN

      Case caze = junkDataHelper.setupJunkCase();

      UacQidLink uacQidLink = new UacQidLink();
      uacQidLink.setId(TEST_UACLINK_ID);
      uacQidLink.setQid(TEST_QID);
      uacQidLink.setUac("abc");
      uacQidLink.setCaze(caze);
      uacQidLink.setActive(true);
      uacQidLinkRepository.saveAndFlush(uacQidLink);

      ReceiptDTO receiptDTO = new ReceiptDTO();
      receiptDTO.setQid(TEST_QID);
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setReceipt(receiptDTO);
      EventDTO event = new EventDTO();
      event.setPayload(payloadDTO);

      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setVersion(EVENT_SCHEMA_VERSION);
      eventHeader.setTopic(INBOUND_RECEIPT_TOPIC);
      junkDataHelper.junkify(eventHeader);
      event.setHeader(eventHeader);

      pubsubHelper.sendMessageToSharedProject(INBOUND_RECEIPT_TOPIC, event);

      //  THEN
      EventDTO caseEmittedEvent = outboundCaseQueueSpy.checkExpectedMessageReceived();

      CaseUpdateDTO emittedCase = caseEmittedEvent.getPayload().getCaseUpdate();
      assertThat(emittedCase.getCaseId()).isEqualTo(caze.getId());
      assertThat(emittedCase.getSample()).isEqualTo(caze.getSample());
      assertThat(emittedCase.isReceiptReceived()).isTrue();

      EventDTO uacUpdatedEvent = outboundUacQueueSpy.checkExpectedMessageReceived();
      UacUpdateDTO emittedUac = uacUpdatedEvent.getPayload().getUacUpdate();
      assertThat(emittedUac.isActive()).isFalse();

      List<uk.gov.ons.ssdc.caseprocessor.model.entity.Event> storedEvents =
          eventRepository.findAll();
      assertThat(storedEvents.size()).isEqualTo(1);
      assertThat(storedEvents.get(0).getUacQidLink().getId()).isEqualTo(TEST_UACLINK_ID);
      assertThat(storedEvents.get(0).getType()).isEqualTo(EventType.RECEIPT);
    }
  }
}
