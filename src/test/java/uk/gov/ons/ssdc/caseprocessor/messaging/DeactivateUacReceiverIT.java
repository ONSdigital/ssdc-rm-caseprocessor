package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DeactivateUacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeactivateUacReceiverIT {
  private static final String TEST_QID = "0123456789";

  private static final String caseRhUacQueue = "events.rh.uacUpdate";

  @Value("${queueconfig.deactivate-uac-queue}")
  private String deactivateUacQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private EventRepository eventRepository;

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(deactivateUacQueue);
    rabbitQueueHelper.purgeQueue(caseRhUacQueue);
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
  }

  @Test
  public void testDeactivateUacReceiver() throws Exception {
    try (QueueSpy uacRhQueue = rabbitQueueHelper.listen(caseRhUacQueue)) {
      // GIVEN
      ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.DEACTIVATE_UAC);
      responseManagementEvent.setEvent(eventDTO);

      PayloadDTO payloadDTO = new PayloadDTO();
      DeactivateUacDTO deactivateUacDTO = new DeactivateUacDTO();
      deactivateUacDTO.setQid(TEST_QID);
      payloadDTO.setDeactivateUac(deactivateUacDTO);
      responseManagementEvent.setPayload(payloadDTO);

      UacQidLink uacQidLink = new UacQidLink();
      uacQidLink.setId(UUID.randomUUID());
      uacQidLink.setQid(TEST_QID);
      uacQidLink.setUac("test_uac");
      uacQidLink.setActive(true);
      uacQidLinkRepository.save(uacQidLink);

      // WHEN
      rabbitQueueHelper.sendMessage(deactivateUacQueue, responseManagementEvent);

      // THEN
      ResponseManagementEvent actualResponseManagementEvent =
          uacRhQueue.checkExpectedMessageReceived();

      UacDTO uac = actualResponseManagementEvent.getPayload().getUac();
      assertThat(uac.getQuestionnaireId()).isEqualTo(TEST_QID);
      assertThat(uac.isActive()).isFalse();

      UacQidLink sentUacQidLinkUpdated = uacQidLinkRepository.findByQid(TEST_QID).get();

      assertThat(sentUacQidLinkUpdated.isActive()).isFalse();

      Event event = eventRepository.findAll().get(0);
      assertThat(event.getEventType()).isEqualTo(EventType.DEACTIVATE_UAC);
    }
  }
}