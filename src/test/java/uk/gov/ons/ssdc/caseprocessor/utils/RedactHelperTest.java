package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.NewCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.TelephoneCaptureDTO;

public class RedactHelperTest {
  @Test
  public void testRedactWorksForMap() {
    // GIVEN
    NewCase newCase = new NewCase();

    newCase.setSampleSensitive(Map.of("PHONE_NUMBER", "999999"));

    // WHEN
    // Cast is required for the test, but when we use this we only want Object anyway
    NewCase newCaseDeepCopy = (NewCase) RedactHelper.redact(newCase);

    // THEN
    assertThat(newCaseDeepCopy.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", "REDACTED"));

    // Extra check to make sure the original object wasn't accidentally mutated
    assertThat(newCase.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", "999999"));
  }

  @Test
  public void testRedactWorksForString() {
    // GIVEN

    TelephoneCaptureDTO telephoneCaptureDto = new TelephoneCaptureDTO();

    telephoneCaptureDto.setUac("SUPER SECRET VALUE");

    PayloadDTO payloadDto = new PayloadDTO();
    payloadDto.setTelephoneCapture(telephoneCaptureDto);

    EventDTO eventDto = new EventDTO();
    eventDto.setPayload(payloadDto);

    // WHEN
    // Cast is required for the test, but when we use this we only want Object anyway
    EventDTO eventDeepCopy = (EventDTO) RedactHelper.redact(eventDto);

    // THEN
    assertThat(eventDeepCopy.getPayload().getTelephoneCapture().getUac()).isEqualTo("REDACTED");

    // Extra check to make sure the original object wasn't accidentally mutated
    assertThat(eventDto.getPayload().getTelephoneCapture().getUac())
        .isEqualTo("SUPER SECRET VALUE");
  }
}
