package uk.gov.ons.ssdc.caseprocessor.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;

public class SampleValidateHelper {

  public static void validateNewValue(
      Entry<String, String> entry, ColumnValidator columnValidator, EventType eventType) {
    if (columnValidator.getColumnName().equals(entry.getKey())) {
      Map<String, String> validateThis = Map.of(entry.getKey(), entry.getValue());

      Optional<String> validationErrors = columnValidator.validateRow(validateThis);
      if (validationErrors.isPresent()) {
        throw new RuntimeException(eventType + " data update failed validation");
      }
    }
  }
}
