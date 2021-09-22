package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EqLaunchDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.JunkDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class EqLaunchReceiverIT {
  private static final String TEST_QID = "1234334";
  private static final String INBOUND_TOPIC = "event_eq-launch";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;
  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private EventRepository eventRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeSharedProjectMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testEqLaunchLogsEventSetsFlagAndEmitsCorrectCaseUpdatedEvent() throws Exception {
    // GIVEN

    try (QueueSpy<EventDTO> outboundCaseQueueSpy =
        pubsubHelper.sharedProjectListen(OUTBOUND_CASE_SUBSCRIPTION, EventDTO.class)) {
      Case caze = junkDataHelper.setupJunkCase();

      UacQidLink uacQidLink = new UacQidLink();
      uacQidLink.setId(UUID.randomUUID());
      uacQidLink.setCaze(caze);
      uacQidLink.setUac("Junk");
      uacQidLink.setQid(TEST_QID);
      uacQidLink.setCaze(caze);
      uacQidLinkRepository.saveAndFlush(uacQidLink);

      EventDTO eqLaunchedEvent = new EventDTO();
      EventHeaderDTO eventHeader = new EventHeaderDTO();
      eventHeader.setVersion(EVENT_SCHEMA_VERSION);
      eventHeader.setTopic(INBOUND_TOPIC);
      junkDataHelper.junkify(eventHeader);
      eqLaunchedEvent.setHeader(eventHeader);

      EqLaunchDTO EqLaunch = new EqLaunchDTO();
      EqLaunch.setQid(uacQidLink.getQid());
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setEqLaunch(EqLaunch);
      eqLaunchedEvent.setPayload(payloadDTO);

      // WHEN
      pubsubHelper.sendMessageToSharedProject(INBOUND_TOPIC, eqLaunchedEvent);

      // THEN
      EventDTO caseUpdatedEvent = outboundCaseQueueSpy.checkExpectedMessageReceived();

      assertThat(caseUpdatedEvent.getPayload().getCaseUpdate().getCaseId()).isEqualTo(caze.getId());
      assertThat(caseUpdatedEvent.getPayload().getCaseUpdate().isEqLaunched()).isTrue();

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isEqualTo(1);
      Event event = events.get(0);
      assertThat(event.getDescription()).isEqualTo("EQ launched");
      UacQidLink actualUacQidLink = event.getUacQidLink();
      assertThat(actualUacQidLink.getQid()).isEqualTo(TEST_QID);
      assertThat(actualUacQidLink.getCaze().getId()).isEqualTo(caze.getId());
    }
  }
}