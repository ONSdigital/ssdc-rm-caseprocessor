package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSampleSensitive;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;

import java.util.Map;

public class RedactHelperTest {
  @Test
  public void testRedactWorks() {
    // GIVEN
    ResponseManagementEvent rme = new ResponseManagementEvent();
    PayloadDTO payload = new PayloadDTO();
    Case caze = new Case();



//    Sample sample = new Sample();

//    sample.setSample(Map.of("PHONE_NUMBER", "999999"));

    caze.setSample(Map.of("PHONE_NUMBER", "999999"));

//    updateSampleSensitive.setSampleSensitive(Map.of("PHONE_NUMBER", "999999"));
    rme.setPayload(payload);

    // WHEN
    // Cast is required for the test, but when we use this we only want Object anyway
    ResponseManagementEvent rmeDeepCopy = (ResponseManagementEvent) RedactHelper.redact(rme);

    // THEN
    assertThat(rmeDeepCopy.getPayload().getUpdateSampleSensitive().getSampleSensitive())
        .isEqualTo(Map.of("REDACTED", "REDACTED"));

    // Extra check to make sure the original object wasn't accidentally mutated
    assertThat(caze.getSample().equals(Map.of("PHONE_NUMBER", "999999")));

  }
}
