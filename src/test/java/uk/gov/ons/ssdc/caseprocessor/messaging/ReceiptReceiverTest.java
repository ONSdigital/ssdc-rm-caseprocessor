package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;

@RunWith(MockitoJUnitRunner.class)
public class ReceiptReceiverTest {
  private static final String QUESTIONNAIRE_ID = "12345";
  private static final UUID CASE_ID = UUID.randomUUID();

  @Mock private UacService uacService;

  @Mock private CaseService caseService;

  @InjectMocks ReceiptReceiver underTest;

  @Test
  public void testUnlinkedUacQidReceiptWhereActive() {
    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setQuestionnaireId(QUESTIONNAIRE_ID);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setResponse(responseDTO);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payloadDTO);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(true);

    when(uacService.findByQid(any())).thenReturn(uacQidLink);

    underTest.receiveMessage(responseManagementEvent);

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService).saveAndEmitUacUpdatedEvent(uacQidLinkCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.isActive()).isFalse();
  }

  @Test
  public void testUnlinkedUacQidReceiptWhereInactive() {
    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setQuestionnaireId(QUESTIONNAIRE_ID);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setResponse(responseDTO);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payloadDTO);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(false);

    when(uacService.findByQid(any())).thenReturn(uacQidLink);

    underTest.receiveMessage(responseManagementEvent);

    ArgumentCaptor<String> uacQidLinkCaptor = ArgumentCaptor.forClass(String.class);
    verify(uacService).findByQid(uacQidLinkCaptor.capture());
    assertThat(uacQidLinkCaptor.getValue()).isEqualTo(QUESTIONNAIRE_ID);

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

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(true);
    Case caze = new Case();
    caze.setId(CASE_ID);
    caze.setReceiptReceived(false);
    uacQidLink.setCaze(caze);

    when(uacService.findByQid(any())).thenReturn(uacQidLink);

    underTest.receiveMessage(responseManagementEvent);

    ArgumentCaptor<UacQidLink> uacQidLinkCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacService).saveAndEmitUacUpdatedEvent(uacQidLinkCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkCaptor.getValue();
    assertThat(actualUacQidLink.isActive()).isFalse();

    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseService).saveCaseAndEmitCaseUpdatedEvent(caseArgumentCaptor.capture());
    Case actualCase = caseArgumentCaptor.getValue();
    assertThat(actualCase.getId()).isEqualTo(caze.getId());
    assertThat(actualCase.isReceiptReceived()).isTrue();
  }
}
