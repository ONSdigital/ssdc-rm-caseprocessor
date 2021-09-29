package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.SMS_FULFILMENT_TOPIC;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EnrichedSmsFulfilment;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.utils.HashHelper;
import uk.gov.ons.ssdc.common.model.entity.Case;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
class SmsFulfilmentReceiverIT {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String PACK_CODE = "TEST_SMS";

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

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
    UacQidDTO smsUacQid = uacQidDTOList.get(0);

    // Create the case
    Case testCase = junkDataHelper.setupJunkCase();

    // Build the event message
    EnrichedSmsFulfilment enrichedSmsFulfilment = new EnrichedSmsFulfilment();
    enrichedSmsFulfilment.setUac(smsUacQid.getUac());
    enrichedSmsFulfilment.setQid(smsUacQid.getQid());
    enrichedSmsFulfilment.setCaseId(testCase.getId());
    enrichedSmsFulfilment.setPackCode(PACK_CODE);

    Map<String, String> uacMetadataMap = new HashMap<>();
    uacMetadataMap.put("Wave of Contact Number", "1");
    enrichedSmsFulfilment.setUacMetadata(uacMetadataMap);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setEnrichedSmsFulfilment(enrichedSmsFulfilment);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    eventHeader.setTopic(SMS_FULFILMENT_TOPIC);
    junkDataHelper.junkify(eventHeader);

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    try (QueueSpy<EventDTO> outboundUacQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      pubsubHelper.sendMessage(SMS_FULFILMENT_TOPIC, event);
      EventDTO emittedEvent = outboundUacQueueSpy.checkExpectedMessageReceived();

      assertThat(emittedEvent.getHeader().getTopic()).isEqualTo(uacUpdateTopic);

      UacUpdateDTO uacUpdatedEvent = emittedEvent.getPayload().getUacUpdate();
      assertThat(uacUpdatedEvent.getCaseId()).isEqualTo(testCase.getId());
      assertThat(uacUpdatedEvent.getUacHash()).isEqualTo(HashHelper.hash(smsUacQid.getUac()));
      assertThat(uacUpdatedEvent.getQid()).isEqualTo(smsUacQid.getQid());
    }
  }
}
