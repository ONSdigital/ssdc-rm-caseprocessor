package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_CORRELATION_ID;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.TEST_ORIGINATING_USER;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.OUTBOUND_EVENT_SCHEMA_VERSION;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.Survey;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;
import uk.gov.ons.ssdc.common.validation.LengthRule;
import uk.gov.ons.ssdc.common.validation.Rule;

public class SampleValidateHelperTest {

  @Test
  public void testValidateNewValue() {
    // Given
    Entry<String, String> sampleUpdateData = Map.entry("firstName", "Test");
    ColumnValidator columnValidator =
        new ColumnValidator("firstName", false, new Rule[]{new LengthRule(60)});

    // When/Then
    assertDoesNotThrow(
        () -> SampleValidateHelper.validateNewValue(sampleUpdateData, columnValidator,
            EventType.UPDATE_SAMPLE));
  }

  @Test
  public void testValidateNewValueError() {
    // Given
    Entry<String, String> sampleUpdateData = Map.entry("firstName", "Test");
    ColumnValidator columnValidator =
        new ColumnValidator("firstName", true, new Rule[]{new LengthRule(1)});

    // When
    RuntimeException thrown =
        assertThrows(RuntimeException.class,
            () -> SampleValidateHelper.validateNewValue(sampleUpdateData, columnValidator,
                EventType.UPDATE_SAMPLE_SENSITIVE));

    // Then
    assertThat(thrown.getMessage()).isEqualTo(
        EventType.UPDATE_SAMPLE +
            " data update failed validation: Column 'firstName' value 'Test' validation" +
            " error: Exceeded max length of 1"
    );
  }
}
