package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_SUBSCRIPTION;

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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.SurveyLaunchDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.PubsubHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SurveyLaunchReceiverIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String TEST_QID = "1234334";
  private static final String INBOUND_TOPIC = "event_survey-launch";

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Autowired private PubsubHelper pubsubHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;

  @BeforeEach
  public void setUp() {
    pubsubHelper.purgeMessages(OUTBOUND_CASE_SUBSCRIPTION, caseUpdateTopic);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testSurveyLaunchLogsEventSetsFlagAndEmitsCorrectCaseUpdatedEvent() throws Exception {
    // GIVEN

    try (QueueSpy<ResponseManagementEvent> outboundCaseQueueSpy =
        pubsubHelper.listen(OUTBOUND_CASE_SUBSCRIPTION, ResponseManagementEvent.class)) {
      Case caze = new Case();
      caze.setId(TEST_CASE_ID);
      caze.setSurveyLaunched(false);
      caze = caseRepository.saveAndFlush(caze);

      UacQidLink uacQidLink = new UacQidLink();
      uacQidLink.setId(UUID.randomUUID());
      uacQidLink.setCaze(caze);
      uacQidLink.setQid(TEST_QID);
      uacQidLinkRepository.saveAndFlush(uacQidLink);

      ResponseManagementEvent surveyLaunchedEvent = new ResponseManagementEvent();
      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.SURVEY_LAUNCH);
      eventDTO.setSource("Respondent Home");
      eventDTO.setChannel("RH");
      surveyLaunchedEvent.setEvent(eventDTO);

      SurveyLaunchDTO surveyLaunch = new SurveyLaunchDTO();
      surveyLaunch.setQid(uacQidLink.getQid());
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setSurveyLaunch(surveyLaunch);
      surveyLaunchedEvent.setPayload(payloadDTO);

      // WHEN
      pubsubHelper.sendMessage(INBOUND_TOPIC, surveyLaunchedEvent);

      // THEN
      ResponseManagementEvent caseUpdatedEvent =
          outboundCaseQueueSpy.checkExpectedMessageReceived();

      assertThat(caseUpdatedEvent.getPayload().getCaseUpdate().getCaseId()).isEqualTo(TEST_CASE_ID);
      assertThat(caseUpdatedEvent.getPayload().getCaseUpdate().isSurveyLaunched()).isTrue();

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isEqualTo(1);
      Event event = events.get(0);
      assertThat(event.getEventDescription()).isEqualTo("Survey launched");
      UacQidLink actualUacQidLink = event.getUacQidLink();
      assertThat(actualUacQidLink.getQid()).isEqualTo(TEST_QID);
      assertThat(actualUacQidLink.getCaze().getId()).isEqualTo(TEST_CASE_ID);
    }
  }
}
