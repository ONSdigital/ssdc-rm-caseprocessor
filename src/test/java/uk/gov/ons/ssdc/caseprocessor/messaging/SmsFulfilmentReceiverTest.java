package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EnrichedSmsFulfilment;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ExtendWith(MockitoExtension.class)
class SmsFulfilmentReceiverTest {

  @InjectMocks SmsFulfilmentReceiver underTest;

  @Mock CaseService caseService;
  @Mock UacService uacService;
  @Mock EventLogger eventLogger;

  private static final UUID CASE_ID = UUID.randomUUID();
  private static final String TEST_QID = "TEST_QID";
  private static final String TEST_UAC = "TEST_UAC";
  private static final String PACK_CODE = "TEST_SMS";
  private static final Map<String, String> TEST_UAC_METADATA = Map.of("TEST_UAC_METADATA", "TEST");

  private static final String SMS_FULFILMENT_DESCRIPTION = "SMS fulfilment request received";

  @Test
  void testReceiveMessageHappyPathWithUacQid() {
    // Given
    Case testCase = new Case();
    testCase.setId(CASE_ID);
    EventDTO event = buildEnrichedSmsFulfilmentEventWithUacQid();
    Message<byte[]> eventMessage = constructMessage(event);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);
    when(uacService.existsByQid(TEST_QID)).thenReturn(false);

    // When
    underTest.receiveMessage(eventMessage);

    // Then
    verify(uacService)
        .createLinkAndEmitNewUacQid(
            testCase,
            TEST_UAC,
            TEST_QID,
            TEST_UAC_METADATA,
            TEST_CORRELATION_ID,
            TEST_ORIGINATING_USER);
    verify(eventLogger)
        .logCaseEvent(
            testCase, SMS_FULFILMENT_DESCRIPTION, EventType.SMS_FULFILMENT, event, eventMessage);
  }

  @Test
  void testReceiveMessageHappyPathNoUacQid() {
    // Given
    Case testCase = new Case();
    testCase.setId(CASE_ID);
    EventDTO event = buildEnrichedSmsFulfilmentEvent();
    Message<byte[]> eventMessage = constructMessage(event);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);

    // When
    underTest.receiveMessage(eventMessage);

    // Then
    verifyNoInteractions(uacService);
    verify(eventLogger)
        .logCaseEvent(
            testCase, SMS_FULFILMENT_DESCRIPTION, EventType.SMS_FULFILMENT, event, eventMessage);
  }

  @Test
  void testReceiveMessageQidAlreadyLinkedToCorrectCase() {
    // Given
    Case testCase = new Case();
    testCase.setId(CASE_ID);
    EventDTO event = buildEnrichedSmsFulfilmentEventWithUacQid();
    Message<byte[]> eventMessage = constructMessage(event);

    UacQidLink existingUacQidLink = new UacQidLink();
    existingUacQidLink.setQid(TEST_QID);
    existingUacQidLink.setCaze(testCase);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);

    when(uacService.existsByQid(TEST_QID)).thenReturn(true);
    when(uacService.findByQid(TEST_QID)).thenReturn(existingUacQidLink);

    // When
    underTest.receiveMessage(eventMessage);

    // Then
    verify(uacService, never()).saveAndEmitUacUpdateEvent(any(), any(UUID.class), anyString());
    verifyNoInteractions(eventLogger);
  }

  @Test
  void testReceiveMessageQidAlreadyLinkedToOtherCase() {
    // Given
    Case testCase = new Case();
    testCase.setId(CASE_ID);

    Case otherCase = new Case();
    otherCase.setId(UUID.randomUUID());

    EventDTO event = buildEnrichedSmsFulfilmentEventWithUacQid();
    Message<byte[]> eventMessage = constructMessage(event);

    UacQidLink existingUacQidLink = new UacQidLink();
    existingUacQidLink.setQid(TEST_QID);
    existingUacQidLink.setCaze(otherCase);

    when(caseService.getCaseByCaseId(CASE_ID)).thenReturn(testCase);

    when(uacService.existsByQid(TEST_QID)).thenReturn(true);
    when(uacService.findByQid(TEST_QID)).thenReturn(existingUacQidLink);

    // When, then throws
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> underTest.receiveMessage(eventMessage));
    assertThat(thrown.getMessage())
        .isEqualTo("SMS fulfilment QID TEST_QID is already linked to a different case");
    verifyNoInteractions(eventLogger);
  }

  private EventDTO buildEnrichedSmsFulfilmentEventWithUacQid() {
    EventDTO event = buildEnrichedSmsFulfilmentEvent();
    event.getPayload().getEnrichedSmsFulfilment().setUac(TEST_UAC);
    event.getPayload().getEnrichedSmsFulfilment().setQid(TEST_QID);
    return event;
  }

  private EventDTO buildEnrichedSmsFulfilmentEvent() {
    EnrichedSmsFulfilment enrichedSmsFulfilment = new EnrichedSmsFulfilment();
    enrichedSmsFulfilment.setCaseId(CASE_ID);
    enrichedSmsFulfilment.setPackCode(PACK_CODE);
    enrichedSmsFulfilment.setUacMetadata(TEST_UAC_METADATA);

    EventHeaderDTO eventHeader = new EventHeaderDTO();
    eventHeader.setVersion(OUTBOUND_EVENT_SCHEMA_VERSION);
    eventHeader.setCorrelationId(TEST_CORRELATION_ID);
    eventHeader.setOriginatingUser(TEST_ORIGINATING_USER);
    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setEnrichedSmsFulfilment(enrichedSmsFulfilment);

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    return event;
  }
}
