package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacAuthenticationDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UacAuthenticationReceiverIT {
  private static final String TEST_QID = "1234334";
  private static final String INBOUND_TOPIC = "event_uac-authentication";

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private EventRepository eventRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;

  @BeforeEach
  public void setUp() {
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testUacAuthenticated() throws Exception {
    // GIVEN
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(TEST_QID);
    uacQidLinkRepository.saveAndFlush(uacQidLink);

    EventDTO surveyLaunchedEvent = new EventDTO();
    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(EVENT_SCHEMA_VERSION);
    eventHeader.setTopic(INBOUND_TOPIC);
    eventHeader.setSource("Respondent Home");
    eventHeader.setChannel("RH");
    surveyLaunchedEvent.setHeader(eventHeader);

    UacAuthenticationDTO uacAuthentication = new UacAuthenticationDTO();
    uacAuthentication.setQid(uacQidLink.getQid());
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUacAuthentication(uacAuthentication);
    surveyLaunchedEvent.setPayload(payloadDTO);

    // WHEN
    pubsubHelper.sendMessageToSharedProject(INBOUND_TOPIC, surveyLaunchedEvent);
    Thread.sleep(2000);

    // THEN
    List<uk.gov.ons.ssdc.caseprocessor.model.entity.Event> events = eventRepository.findAll();
    assertThat(events.size()).isEqualTo(1);
    uk.gov.ons.ssdc.caseprocessor.model.entity.Event event = events.get(0);
    assertThat(event.getDescription()).isEqualTo("Respondent authenticated");
    UacQidLink actualUacQidLink = event.getUacQidLink();
    assertThat(actualUacQidLink.getQid()).isEqualTo(TEST_QID);
  }
}
