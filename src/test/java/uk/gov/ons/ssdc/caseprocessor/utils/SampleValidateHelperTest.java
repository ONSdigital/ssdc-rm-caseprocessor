package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;
import uk.gov.ons.ssdc.common.validation.LengthRule;
import uk.gov.ons.ssdc.common.validation.Rule;

class SampleValidateHelperTest {

  @Test
  void testValidateNewValue() {
    // Given
    Entry<String, String> sampleUpdateData = Map.entry("testSampleField", "Test");
    ColumnValidator columnValidator =
        new ColumnValidator("testSampleField", false, new Rule[] {new LengthRule(60)});

    // When/Then
    assertDoesNotThrow(
        () ->
            SampleValidateHelper.validateNewValue(
                sampleUpdateData, columnValidator, EventType.UPDATE_SAMPLE));
  }

  @Test
  void testValidateNewValueError() {
    // Given
    Entry<String, String> sampleUpdateData = Map.entry("testSampleField", "Test");
    ColumnValidator columnValidator =
        new ColumnValidator("testSampleField", true, new Rule[] {new LengthRule(1)});

    // When
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                SampleValidateHelper.validateNewValue(
                    sampleUpdateData, columnValidator, EventType.UPDATE_SAMPLE_SENSITIVE));

    // Then
    assertThat(thrown.getMessage())
        .isEqualTo("UPDATE_SAMPLE_SENSITIVE failed validation for column name: testSampleField");
  }
}
