package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.RefusalType;

@ExtendWith(MockitoExtension.class)
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

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    eventHeader.setCorrelationId(TEST_CORRELATION_ID);
    eventHeader.setOriginatingUser(TEST_ORIGINATING_USER);
    eventHeader.setTopic("Test topic");
    eventHeader.setDateTime(OffsetDateTime.now(ZoneId.of("UTC")));

    EventDTO event = new EventDTO();
    event.setPayload(payloadDTO);
    event.setHeader(eventHeader);
    Message<byte[]> message = constructMessage(event);

    Case caze = new Case();
    caze.setId(CASE_ID);
    caze.setRefusalReceived(null);

    when(caseService.getCase(CASE_ID)).thenReturn(caze);

    // When
    underTest.receiveMessage(message);

    // Then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseService)
        .saveCaseAndEmitCaseUpdate(
            caseArgumentCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    Case actualCase = caseArgumentCaptor.getValue();

    assertThat(actualCase.getId()).isEqualTo(CASE_ID);
    assertThat(actualCase.getRefusalReceived()).isEqualTo(RefusalType.HARD_REFUSAL);

    verify(eventLogger)
        .logCaseEvent(
            eq(caze), eq("Refusal Received"), eq(EventType.REFUSAL), eq(event), eq(message));
  }
}
