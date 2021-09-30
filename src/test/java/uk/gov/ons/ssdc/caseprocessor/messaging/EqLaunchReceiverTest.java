package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessage;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.EVENT_SCHEMA_VERSION;

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
import uk.gov.ons.ssdc.caseprocessor.model.dto.EqLaunchDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@ExtendWith(MockitoExtension.class)
public class EqLaunchReceiverTest {

  private final UUID TEST_CASE_ID = UUID.randomUUID();
  private final String TEST_QID_ID = "1234567890123456";

  @Mock private UacService uacService;
  @Mock private EventLogger eventLogger;
  @Mock private CaseService caseService;

  @InjectMocks EqLaunchReceiver underTest;

  @Test
  public void testEqLaunchedEventFromRH() {
    EventDTO managementEvent = new EventDTO();
    managementEvent.setHeader(new EventHeaderDTO());
    managementEvent.getHeader().setVersion(EVENT_SCHEMA_VERSION);
    managementEvent.getHeader().setCorrelationId(TEST_CORRELATION_ID);
    managementEvent.getHeader().setOriginatingUser(TEST_ORIGINATING_USER);
    managementEvent.getHeader().setDateTime(OffsetDateTime.now(ZoneId.of("UTC")));
    managementEvent.getHeader().setTopic("Test topic");
    managementEvent.getHeader().setChannel("RH");
    managementEvent.setPayload(new PayloadDTO());

    EqLaunchDTO eqLaunch = new EqLaunchDTO();
    eqLaunch.setQid(TEST_QID_ID);
    managementEvent.getPayload().setEqLaunch(eqLaunch);

    UacQidLink expectedUacQidLink = new UacQidLink();
    expectedUacQidLink.setUac(TEST_QID_ID);
    Case caze = new Case();
    caze.setId(TEST_CASE_ID);
    expectedUacQidLink.setCaze(caze);
    Message<byte[]> message = constructMessage(managementEvent);

    // Given
    when(uacService.findByQid(TEST_QID_ID)).thenReturn(expectedUacQidLink);

    // when
    underTest.receiveMessage(message);

    // then
    verify(uacService).findByQid(TEST_QID_ID);

    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseService)
        .saveCaseAndEmitCaseUpdate(
            caseArgumentCaptor.capture(), eq(TEST_CORRELATION_ID), eq(TEST_ORIGINATING_USER));
    assertThat(caseArgumentCaptor.getValue().getId()).isEqualTo(TEST_CASE_ID);
    assertThat(caseArgumentCaptor.getValue().isEqLaunched()).isTrue();

    verify(eventLogger)
        .logUacQidEvent(
            eq(expectedUacQidLink),
            eq("EQ launched"),
            eq(EventType.EQ_LAUNCH),
            eq(managementEvent),
            eq(message));

    verifyNoMoreInteractions(uacService);
    verifyNoMoreInteractions(caseService);
    verifyNoMoreInteractions(eventLogger);
  }
}
