package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;

public class RedactHelperTest {
  @Test
  public void testRedactWorks() {
    // GIVEN
    Sample sample = new Sample();

    sample.setSampleSensitive(Map.of("PHONE_NUMBER", "999999"));

    // WHEN
    // Cast is required for the test, but when we use this we only want Object anyway
    Sample sampleDeepCopy = (Sample) RedactHelper.redact(sample);

    // THEN
    assertThat(sampleDeepCopy.getSampleSensitive()).isEqualTo(Map.of("REDACTED", "REDACTED"));

    // Extra check to make sure the original object wasn't accidentally mutated
    assertThat(sample.getSampleSensitive()).isEqualTo(Map.of("PHONE_NUMBER", "999999"));
  }
}
