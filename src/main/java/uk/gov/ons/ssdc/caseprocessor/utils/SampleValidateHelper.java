package uk.gov.ons.ssdc.caseprocessor.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;

public class SampleValidateHelper {

  public static void validateNewValue(String columnName, String validateThis, ColumnValidator columnValidator, EventType eventType) {

    if (columnValidator.getColumnName().equals(columnName)) {
      Optional<String> validationErrors = columnValidator.validateData(validateThis, true);
      if (validationErrors.isPresent()) {
        throw new RuntimeException(eventType + " event: " + validationErrors.get());
      }
    }
  }
}
