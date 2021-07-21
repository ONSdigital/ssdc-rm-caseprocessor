package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUTBOUND_CASE_QUEUE;

import java.util.List;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.testutils.DeleteDataHelper;
import uk.gov.ons.ssdc.caseprocessor.testutils.QueueSpy;
import uk.gov.ons.ssdc.caseprocessor.testutils.RabbitQueueHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SurveyLaunchedReceiverIT {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String TEST_QID = "1234334";
  private static final String TEST_UAC = "9434343";

  @Value("${queueconfig.survey-launched-queue}")
  private String inboundQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private DeleteDataHelper deleteDataHelper;

  @Autowired private CaseRepository caseRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;

  @Before
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundQueue);
    rabbitQueueHelper.purgeQueue(OUTBOUND_CASE_QUEUE);
    deleteDataHelper.deleteAllData();
  }

  @Test
  public void testSurveyLaunchLogsEventSetsFlagAndEmitsCorrectCaseUpdatedEvent() throws Exception {
    // GIVEN

    try (QueueSpy outboundCaseQueueSpy = rabbitQueueHelper.listen(OUTBOUND_CASE_QUEUE)) {
      Case caze = new Case();
      caze.setId(TEST_CASE_ID);
      caze.setUacQidLinks(null);
      caze.setAddressInvalid(false);
      caze.setRefusalReceived(null);
      caze.setReceiptReceived(false);
      caze.setSurveyLaunched(false);
      caze = caseRepository.saveAndFlush(caze);

      UacQidLink uacQidLink = new UacQidLink();
      uacQidLink.setId(UUID.randomUUID());
      uacQidLink.setCaze(caze);
      uacQidLink.setQid(TEST_QID);
      uacQidLink.setUac(TEST_UAC);
      uacQidLinkRepository.saveAndFlush(uacQidLink);

      ResponseManagementEvent surveyLaunchedEvent = new ResponseManagementEvent();
      EventDTO eventDTO = new EventDTO();
      eventDTO.setType(EventTypeDTO.SURVEY_LAUNCHED);
      eventDTO.setSource("Respondent Home");
      eventDTO.setChannel("RH");
      surveyLaunchedEvent.setEvent(eventDTO);

      ResponseDTO responseDTO = new ResponseDTO();
      responseDTO.setQuestionnaireId(uacQidLink.getQid());
      PayloadDTO payloadDTO = new PayloadDTO();
      payloadDTO.setResponse(responseDTO);
      surveyLaunchedEvent.setPayload(payloadDTO);

      surveyLaunchedEvent.getPayload().getResponse().setQuestionnaireId(uacQidLink.getQid());

      // WHEN
      rabbitQueueHelper.sendMessage(inboundQueue, surveyLaunchedEvent);

      // THEN
      ResponseManagementEvent caseUpdatedEvent =
          outboundCaseQueueSpy.checkExpectedMessageReceived();

      assertThat(caseUpdatedEvent.getPayload().getCollectionCase().getCaseId())
          .isEqualTo(TEST_CASE_ID);
      assertThat(caseUpdatedEvent.getPayload().getCollectionCase().isSurveyLaunched()).isTrue();

      List<Event> events = eventRepository.findAll();
      assertThat(events.size()).isEqualTo(1);
      Event event = events.get(0);
      assertThat(event.getEventDescription()).isEqualTo("Survey launched");
      UacQidLink actualUacQidLink = event.getUacQidLink();
      assertThat(actualUacQidLink.getQid()).isEqualTo(TEST_QID);
      assertThat(actualUacQidLink.getUac()).isEqualTo(TEST_UAC);
      assertThat(actualUacQidLink.getCaze().getId()).isEqualTo(TEST_CASE_ID);
    }
  }
}
