package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.TelephoneCaptureDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@RunWith(MockitoJUnitRunner.class)
public class TelephoneCaptureReceiverTest {

  @InjectMocks TelephoneCaptureReceiver underTest;

  @Mock CaseService caseService;
  @Mock UacService uacService;
  @Mock EventLogger eventLogger;

  private static final UUID CASE_ID = UUID.randomUUID();
  private static final String TEST_QID = "TEST_QID";
  private static final String TEST_UAC = "TEST_UAC";

  private static final String TELEPHONE_CAPTURE_DESCRIPTION = "Telephone capture request received";

  @Test
  public void testReceiveMessageHappyPath() {
    // Given
    Case testCase = new Case();
    testCase.setId(CASE_ID);
    ResponseManagementEvent responseManagementEvent = buildTelephoneCaptureEvent();
    Message<ResponseManagementEvent> eventMessage =
        constructMessageWithValidTimeStamp(responseManagementEvent);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);
    when(uacService.existsByQid(TEST_QID)).thenReturn(false);

    // When
    underTest.receiveMessage(eventMessage);

    // Then
    verify(uacService).createNewUacQidLink(testCase, TEST_UAC, TEST_QID);
    verify(eventLogger)
        .logCaseEvent(
            testCase,
            responseManagementEvent.getEvent().getDateTime(),
            TELEPHONE_CAPTURE_DESCRIPTION,
            EventType.TELEPHONE_CAPTURE_REQUESTED,
            responseManagementEvent.getEvent(),
            responseManagementEvent.getPayload().getTelephoneCapture(),
            getMsgTimeStamp(eventMessage));
  }

  private ResponseManagementEvent buildTelephoneCaptureEvent() {
    TelephoneCaptureDTO telephoneCaptureDTO = new TelephoneCaptureDTO();
    telephoneCaptureDTO.setCaseId(CASE_ID);
    telephoneCaptureDTO.setQid(TEST_QID);
    telephoneCaptureDTO.setUac(TEST_UAC);

    EventDTO eventDTO = new EventDTO();
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setTelephoneCapture(telephoneCaptureDTO);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(eventDTO);
    responseManagementEvent.setPayload(payloadDTO);

    return responseManagementEvent;
  }
}
