package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.TelephoneCaptureDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@ExtendWith(MockitoExtension.class)
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
    Message<byte[]> eventMessage = constructMessageWithValidTimeStamp(responseManagementEvent);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);
    when(uacService.existsByQid(TEST_QID)).thenReturn(false);

    // When
    underTest.receiveMessage(eventMessage);

    // Then
    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService).saveAndEmitUacUpdateEvent(uacQidLinkCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.isActive()).isTrue();
    assertThat(actualUacQidLink.getQid()).isEqualTo(TEST_QID);
    assertThat(actualUacQidLink.getUac()).isEqualTo(TEST_UAC);
    assertThat(actualUacQidLink.getCaze()).isEqualTo(testCase);

    verify(eventLogger)
        .logCaseEvent(
            testCase,
            responseManagementEvent.getEvent().getDateTime(),
            TELEPHONE_CAPTURE_DESCRIPTION,
            EventType.TELEPHONE_CAPTURE,
            responseManagementEvent.getEvent(),
            responseManagementEvent.getPayload().getTelephoneCapture(),
            getMsgTimeStamp(eventMessage));
  }

  @Test
  public void testReceiveMessageQidAlreadyLinkedToCorrectCase() {
    // Given
    Case testCase = new Case();
    testCase.setId(CASE_ID);
    ResponseManagementEvent responseManagementEvent = buildTelephoneCaptureEvent();
    Message<byte[]> eventMessage = constructMessageWithValidTimeStamp(responseManagementEvent);

    UacQidLink existingUacQidLink = new UacQidLink();
    existingUacQidLink.setQid(TEST_QID);
    existingUacQidLink.setCaze(testCase);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);

    when(uacService.existsByQid(TEST_QID)).thenReturn(true);
    when(uacService.findByQid(TEST_QID)).thenReturn(existingUacQidLink);

    // When
    underTest.receiveMessage(eventMessage);

    // Then
    verify(uacService, never()).saveAndEmitUacUpdateEvent(any());
    verify(eventLogger, never()).logCaseEvent(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testReceiveMessageQidAlreadyLinkedToOtherCase() {
    // Given
    Case testCase = new Case();
    testCase.setId(CASE_ID);

    Case otherCase = new Case();
    otherCase.setId(UUID.randomUUID());

    ResponseManagementEvent responseManagementEvent = buildTelephoneCaptureEvent();
    Message<byte[]> eventMessage = constructMessageWithValidTimeStamp(responseManagementEvent);

    UacQidLink existingUacQidLink = new UacQidLink();
    existingUacQidLink.setQid(TEST_QID);
    existingUacQidLink.setCaze(otherCase);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);

    when(uacService.existsByQid(TEST_QID)).thenReturn(true);
    when(uacService.findByQid(TEST_QID)).thenReturn(existingUacQidLink);

    // When, then throws
    assertThrows(RuntimeException.class, () -> underTest.receiveMessage(eventMessage));
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
