package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;

import java.time.OffsetDateTime;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper;

@ExtendWith(MockitoExtension.class)
public class ReceiptReceiverTest {
  private static final String QUESTIONNAIRE_ID = "12345";
  private static final UUID CASE_ID = UUID.randomUUID();

  @Mock private UacService uacService;
  @Mock private CaseService caseService;
  @Mock private EventLogger eventLogger;

  @InjectMocks ReceiptReceiver underTest;

  @Test
  public void testUnlinkedUacQidReceiptWhereActive() {
    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setQuestionnaireId(QUESTIONNAIRE_ID);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setResponse(responseDTO);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payloadDTO);

    EventDTO eventDTO = new EventDTO();
    eventDTO.setType(EventTypeDTO.RESPONSE_RECEIVED);
    eventDTO.setDateTime(OffsetDateTime.now());
    responseManagementEvent.setEvent(eventDTO);
    Message<ResponseManagementEvent> message =
        constructMessageWithValidTimeStamp(responseManagementEvent);
    OffsetDateTime expectedDateTime = MsgDateHelper.getMsgTimeStamp(message);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(true);

    when(uacService.findByQid(any())).thenReturn(uacQidLink);
    when(uacService.saveAndEmitUacUpdatedEvent(any(UacQidLink.class))).thenReturn(uacQidLink);

    underTest.receiveMessage(message);

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService).saveAndEmitUacUpdatedEvent(uacQidLinkCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.isActive()).isFalse();

    verify(eventLogger)
        .logUacQidEvent(
            eq(actualUacQidLink),
            any(),
            eq("QID Receipted"),
            eq(EventType.RESPONSE_RECEIVED),
            eq(responseManagementEvent.getEvent()),
            eq(responseManagementEvent.getPayload()),
            eq(expectedDateTime));
  }

  @Test
  public void testUnlinkedUacQidReceiptWhereInactive() {
    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setQuestionnaireId(QUESTIONNAIRE_ID);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setResponse(responseDTO);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payloadDTO);

    EventDTO eventDTO = new EventDTO();
    eventDTO.setType(EventTypeDTO.RESPONSE_RECEIVED);
    eventDTO.setDateTime(OffsetDateTime.now());
    responseManagementEvent.setEvent(eventDTO);
    Message<ResponseManagementEvent> message =
        constructMessageWithValidTimeStamp(responseManagementEvent);
    OffsetDateTime expectedDateTime = MsgDateHelper.getMsgTimeStamp(message);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(false);

    when(uacService.findByQid(any())).thenReturn(uacQidLink);

    underTest.receiveMessage(message);

    ArgumentCaptor<String> uacQidLinkCaptor = ArgumentCaptor.forClass(String.class);
    verify(uacService).findByQid(uacQidLinkCaptor.capture());

    verify(eventLogger)
        .logUacQidEvent(
            eq(uacQidLink),
            any(),
            eq("QID Receipted"),
            eq(EventType.RESPONSE_RECEIVED),
            eq(responseManagementEvent.getEvent()),
            eq(responseManagementEvent.getPayload()),
            eq(expectedDateTime));

    verifyNoMoreInteractions(uacService);
    verifyNoInteractions(caseService);
  }

  @Test
  public void testUnlinkedUacQidReceiptsUacQidAndCase() {
    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setQuestionnaireId(QUESTIONNAIRE_ID);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setResponse(responseDTO);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payloadDTO);

    EventDTO eventDTO = new EventDTO();
    eventDTO.setType(EventTypeDTO.RESPONSE_RECEIVED);
    eventDTO.setDateTime(OffsetDateTime.now());
    responseManagementEvent.setEvent(eventDTO);
    Message<ResponseManagementEvent> message =
        constructMessageWithValidTimeStamp(responseManagementEvent);
    OffsetDateTime expectedDateTime = MsgDateHelper.getMsgTimeStamp(message);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(true);
    Case caze = new Case();
    caze.setId(CASE_ID);
    caze.setReceiptReceived(false);
    uacQidLink.setCaze(caze);

    when(uacService.findByQid(any())).thenReturn(uacQidLink);
    when(uacService.saveAndEmitUacUpdatedEvent(any(UacQidLink.class))).thenReturn(uacQidLink);

    underTest.receiveMessage(message);

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService).saveAndEmitUacUpdatedEvent(uacQidLinkCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.isActive()).isFalse();

    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseService).saveCaseAndEmitCaseUpdatedEvent(caseArgumentCaptor.capture());
    Case actualCase = caseArgumentCaptor.getValue();
    assertThat(actualCase.getId()).isEqualTo(caze.getId());
    assertThat(actualCase.isReceiptReceived()).isTrue();

    verify(eventLogger)
        .logUacQidEvent(
            eq(actualUacQidLink),
            eq(responseManagementEvent.getEvent().getDateTime()),
            eq("QID Receipted"),
            eq(EventType.RESPONSE_RECEIVED),
            eq(responseManagementEvent.getEvent()),
            eq(responseManagementEvent.getPayload()),
            eq(expectedDateTime));
  }
}
