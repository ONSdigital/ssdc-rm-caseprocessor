package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.entity.RefusalType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper;

@RunWith(MockitoJUnitRunner.class)
public class RefusalReceiverTest {

  private static final UUID CASE_ID = UUID.randomUUID();

  @InjectMocks RefusalReceiver underTest;

  @Mock CaseService caseService;
  @Mock EventLogger eventLogger;

  @Test
  public void testRefusal() {
    // Given
    RefusalDTO refusalDTO = new RefusalDTO();
    refusalDTO.setCaseId(CASE_ID);
    refusalDTO.setType(RefusalTypeDTO.HARD_REFUSAL);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setRefusal(refusalDTO);

    EventDTO eventDTO = new EventDTO();
    eventDTO.setType(EventTypeDTO.REFUSAL_RECEIVED);
    eventDTO.setDateTime(OffsetDateTime.now());

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(payloadDTO);
    responseManagementEvent.setEvent(eventDTO);
    Message<ResponseManagementEvent> message =
        constructMessageWithValidTimeStamp(responseManagementEvent);
    OffsetDateTime expectedDateTime = MsgDateHelper.getMsgTimeStamp(message);

    Case caze = new Case();
    caze.setId(CASE_ID);
    caze.setRefusalReceived(null);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(caze);

    // When
    underTest.receiveMessage(message);

    // Then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseService).saveCaseAndEmitCaseUpdatedEvent(caseArgumentCaptor.capture());
    Case actualCase = caseArgumentCaptor.getValue();

    assertThat(actualCase.getId()).isEqualTo(CASE_ID);
    assertThat(actualCase.getRefusalReceived()).isEqualTo(RefusalType.HARD_REFUSAL);

    verify(eventLogger)
        .logCaseEvent(
            eq(caze),
            any(),
            eq("Refusal Received"),
            eq(EventType.REFUSAL_RECEIVED),
            eq(responseManagementEvent.getEvent()),
            eq(responseManagementEvent.getPayload()),
            eq(expectedDateTime));
  }
}
