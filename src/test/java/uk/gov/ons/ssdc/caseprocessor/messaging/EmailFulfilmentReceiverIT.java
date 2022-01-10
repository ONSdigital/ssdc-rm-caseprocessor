package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.EMAIL_CONFIRMATION_TOPIC;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.util.List;
import java.util.Map;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EmailConfirmation;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
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
class EmailFulfilmentReceiverIT {

  private static final String PACK_CODE = "TEST_EMAIL";
  private static final Map<String, String> TEST_UAC_METADATA = Map.of("TEST_UAC_METADATA", "TEST");

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
  void testEmailFulfilment() throws Exception {
    // Given
    // Get a new UAC QID pair
    List<UacQidDTO> uacQidDTOList = uacQidServiceClient.getUacQids(1, 1);
    UacQidDTO emailUacQid = uacQidDTOList.get(0);

    // Create the case
    Case testCase = junkDataHelper.setupJunkCase();

    // Build the event message
    EmailConfirmation emailConfirmation = new EmailConfirmation();
    emailConfirmation.setUac(emailUacQid.getUac());
    emailConfirmation.setQid(emailUacQid.getQid());
    emailConfirmation.setCaseId(testCase.getId());
    emailConfirmation.setPackCode(PACK_CODE);
    emailConfirmation.setUacMetadata(TEST_UAC_METADATA);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setEmailConfirmation(emailConfirmation);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    eventHeader.setTopic(EMAIL_CONFIRMATION_TOPIC);
    junkDataHelper.junkify(eventHeader);

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    try (QueueSpy<EventDTO> outboundUacQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      pubsubHelper.sendMessage(EMAIL_CONFIRMATION_TOPIC, event);
      EventDTO emittedEvent = outboundUacQueueSpy.checkExpectedMessageReceived();

      assertThat(emittedEvent.getHeader().getTopic()).isEqualTo(uacUpdateTopic);

      UacUpdateDTO uacUpdatedEvent = emittedEvent.getPayload().getUacUpdate();
      assertThat(uacUpdatedEvent.getCaseId()).isEqualTo(testCase.getId());
      assertThat(uacUpdatedEvent.getUacHash()).isEqualTo(HashHelper.hash(emailUacQid.getUac()));
      assertThat(uacUpdatedEvent.getQid()).isEqualTo(emailUacQid.getQid());
    }

    List<UacQidLink> uacQidLinks = uacQidLinkRepository.findAll();
    assertThat(uacQidLinks.size()).isEqualTo(1);
    assertThat(uacQidLinks.get(0).getCaze().getId()).isEqualTo(testCase.getId());
    assertThat(uacQidLinks.get(0).getMetadata()).isEqualTo(TEST_UAC_METADATA);
  }
}
