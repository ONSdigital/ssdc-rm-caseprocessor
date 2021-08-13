package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_UAC_SUBSCRIPTION;

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
import uk.gov.ons.ssdc.caseprocessor.model.dto.DeactivateUacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DeactivateUacReceiverIT {
  private static final String TEST_QID = "0123456789";

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Value("${queueconfig.deactivate-uac-topic}")
  private String deactivateUacTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private EventRepository eventRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_UAC_SUBSCRIPTION, uacUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testDeactivateUacReceiver() throws Exception {
    try (QueueSpy<EventDTO> uacRhQueue =
        pubsubHelper.listen(OUTBOUND_UAC_SUBSCRIPTION, EventDTO.class)) {
      // GIVEN
      EventDTO event = new EventDTO();
      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setTopic(deactivateUacTopic);
      event.setHeader(eventHeader);

      PayloadDTO payloadDTO = new PayloadDTO();
      DeactivateUacDTO deactivateUacDTO = new DeactivateUacDTO();
      deactivateUacDTO.setQid(TEST_QID);
      payloadDTO.setDeactivateUac(deactivateUacDTO);
      event.setPayload(payloadDTO);

      UacQidLink uacQidLink = new UacQidLink();
      uacQidLink.setId(UUID.randomUUID());
      uacQidLink.setQid(TEST_QID);
      uacQidLink.setUac("test_uac");
      uacQidLink.setActive(true);
      uacQidLinkRepository.save(uacQidLink);

      // WHEN
      pubsubHelper.sendMessage(deactivateUacTopic, event);

      // THEN
      EventDTO actualEvent = uacRhQueue.checkExpectedMessageReceived();

      UacUpdateDTO uac = actualEvent.getPayload().getUacUpdate();
      assertThat(uac.getQid()).isEqualTo(TEST_QID);
      assertThat(uac.isActive()).isFalse();

      UacQidLink sentUacQidLinkUpdated = uacQidLinkRepository.findByQid(TEST_QID).get();

      assertThat(sentUacQidLinkUpdated.isActive()).isFalse();

      uk.gov.ons.ssdc.caseprocessor.model.entity.Event databaseEvent =
          eventRepository.findAll().get(0);
      assertThat(databaseEvent.getType()).isEqualTo(EventType.DEACTIVATE_UAC);
    }
  }
}
