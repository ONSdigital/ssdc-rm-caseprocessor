package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO.UPDATE_SAMPLE_SENSITIVE;
import static uk.gov.ons.ssdc.caseprocessor.testutils.MessageConstructor.constructMessageWithValidTimeStamp;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSampleSensitive;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.utils.RedactHelper;

@ExtendWith(MockitoExtension.class)
public class UpdateSampleSensitiveReceiverTest {

  @Mock private CaseService caseService;
  @Mock private EventLogger eventLogger;

  @InjectMocks UpdateSampleSensitiveReceiver underTest;

  @Test
  public void testUpdateSampleSensitiveReceiver() {
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setDateTime(OffsetDateTime.now().minusHours(1));
    managementEvent.getEvent().setType(UPDATE_SAMPLE_SENSITIVE);
    managementEvent.getEvent().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setUpdateSampleSensitive(new UpdateSampleSensitive());
    managementEvent.getPayload().getUpdateSampleSensitive().setCaseId(UUID.randomUUID());
    managementEvent
        .getPayload()
        .getUpdateSampleSensitive()
        .setSampleSensitive(Map.of("PHONE_NUMBER", "9999999"));
    Message<ResponseManagementEvent> message = constructMessageWithValidTimeStamp(managementEvent);

    // Given
    Case expectedCase = new Case();
    Map<String, String> sensitiveData = new HashMap<>();
    sensitiveData.put("PHONE_NUMBER", "1111111");
    expectedCase.setSampleSensitive(sensitiveData);
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // when
    underTest.receiveMessage(message);

    // then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);

    verify(caseService).saveCase(caseArgumentCaptor.capture());
    Case actualCase = caseArgumentCaptor.getValue();
    assertThat(actualCase.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", "9999999"));

    OffsetDateTime messageDateTime = getMsgTimeStamp(message);

    verify(eventLogger)
        .logCaseEvent(
            eq(expectedCase),
            eq(managementEvent.getEvent().getDateTime()),
            eq("Sensitive data updated"),
            eq(EventType.UPDATE_SAMPLE_SENSITIVE),
            eq(managementEvent.getEvent()),
            eq(RedactHelper.redact(managementEvent.getPayload())),
            eq(messageDateTime));
  }

  @Test
  public void testMessageKeyDoesNotMatchExistingEntry() {
    ResponseManagementEvent managementEvent = new ResponseManagementEvent();
    managementEvent.setEvent(new EventDTO());
    managementEvent.getEvent().setDateTime(OffsetDateTime.now().minusHours(1));
    managementEvent.getEvent().setType(UPDATE_SAMPLE_SENSITIVE);
    managementEvent.getEvent().setChannel("CC");
    managementEvent.setPayload(new PayloadDTO());
    managementEvent.getPayload().setUpdateSampleSensitive(new UpdateSampleSensitive());
    managementEvent.getPayload().getUpdateSampleSensitive().setCaseId(UUID.randomUUID());
    managementEvent
        .getPayload()
        .getUpdateSampleSensitive()
        .setSampleSensitive(Map.of("UPRN", "9999999"));
    Message<ResponseManagementEvent> message = constructMessageWithValidTimeStamp(managementEvent);

    // Given
    Case expectedCase = new Case();
    Map<String, String> sensitiveData = new HashMap<>();
    sensitiveData.put("PHONE_NUMBER", "1111111");
    expectedCase.setSampleSensitive(sensitiveData);
    when(caseService.getCaseByCaseId(any(UUID.class))).thenReturn(expectedCase);

    // When, then throws
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          underTest.receiveMessage(message);
        });
  }
}
