package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TELEPHONE_CAPTURE_TOPIC;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

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
import uk.gov.ons.ssdc.caseprocessor.client.UacQidServiceClient;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.TelephoneCaptureDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.utils.HashHelper;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
class TelephoneCaptureReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private UacQidLinkRepository uacQidLinkRepository;

  @Autowired private UacQidServiceClient uacQidServiceClient;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  void testSmsFulfilment() throws Exception {
    // Given
    // Get a new UAC QID pair
    List<UacQidDTO> uacQidDTOList = uacQidServiceClient.getUacQids(1, 1);
    UacQidDTO telephoneCaptureUacQid = uacQidDTOList.get(0);

    // Create the case
    Case testCase = junkDataHelper.setupJunkCase();

    // Build the event message
    TelephoneCaptureDTO telephoneCaptureDTO = new TelephoneCaptureDTO();
    telephoneCaptureDTO.setUac(telephoneCaptureUacQid.getUac());
    telephoneCaptureDTO.setQid(telephoneCaptureUacQid.getQid());
    telephoneCaptureDTO.setCaseId(testCase.getId());
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setTelephoneCapture(telephoneCaptureDTO);
    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    eventHeader.setTopic(TELEPHONE_CAPTURE_TOPIC);
    junkDataHelper.junkify(eventHeader);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    try (QueueSpy<EventDTO> outboundUacQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      pubsubHelper.sendMessage(TELEPHONE_CAPTURE_TOPIC, event);
      EventDTO emittedEvent = outboundUacQueueSpy.checkExpectedMessageReceived();

      assertThat(emittedEvent.getHeader().getTopic()).isEqualTo(uacUpdateTopic);

      UacUpdateDTO uacUpdatedEvent = emittedEvent.getPayload().getUacUpdate();
      assertThat(uacUpdatedEvent.getCaseId()).isEqualTo(testCase.getId());
      assertThat(uacUpdatedEvent.getUacHash())
          .isEqualTo(HashHelper.hash(telephoneCaptureUacQid.getUac()));
      assertThat(uacUpdatedEvent.getQid()).isEqualTo(telephoneCaptureUacQid.getQid());
    }

    List<UacQidLink> uacQidLinks = uacQidLinkRepository.findAll();
    assertThat(uacQidLinks.size()).isEqualTo(1);
    assertThat(uacQidLinks.get(0).getCaze().getId()).isEqualTo(testCase.getId());
    assertThat(uacQidLinks.get(0).getMetadata()).isNull();
  }
}
