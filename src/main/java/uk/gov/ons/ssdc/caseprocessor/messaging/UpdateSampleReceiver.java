package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSample;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;

@MessageEndpoint
public class UpdateSampleReceiver {

  private final CaseService caseService;
  private final EventLogger eventLogger;

  public UpdateSampleReceiver(CaseService caseService, EventLogger eventLogger) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "updateSampleInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    UpdateSample updateSample = event.getPayload().getUpdateSample();

    Case caze = caseService.getCaseByCaseId(updateSample.getCaseId());

    for (Entry<String, String> entry : updateSample.getSample().entrySet()) {
      // First, validate that only non-sensitive data that is defined is being attempted to be updated
      validateOnlyNonSensitiveDataBeingUpdated(caze, entry);

      // Second, do not allow the data to be blanked
      if (entry.getValue().length() == 0) {
        throw new RuntimeException(
            "Cannot update non-sensitive sample data to blank (" + entry.getKey() + ")");
      }

      // Finally, validate the updated value according to the rules for the column
      for (ColumnValidator columnValidator :
          caze.getCollectionExercise().getSurvey().getSampleValidationRules()) {
        validateNewValue(entry, columnValidator, "Non-sensitive");
      }
    }

    caseService.saveCase(caze);

    eventLogger.logCaseEvent(
        caze, "Non-sensitive sample data updated", EventType.UPDATE_SAMPLE_SENSITIVE, event,
        message);  // TODO: Add new type for UPDATE_SAMPLE
  }


  // TODO: Move this so it's generic?
  private void validateNewValue(
      Entry<String, String> entry, ColumnValidator columnValidator,
      String sampleDataType) {
    if (columnValidator.getColumnName().equals(entry.getKey())) {
      Map<String, String> validateThis = Map.of(entry.getKey(), entry.getValue());

      Optional<String> validationErrors = columnValidator.validateRow(validateThis);
      if (validationErrors.isPresent()) {
        throw new RuntimeException(
            sampleDataType + " data update failed validation: " + validationErrors.get());
      }
    }
  }

//  private void validateOnlyNonSensitiveDataBeingUpdated(Case caze, Entry<String, String> entry) {
//    if (caze.getSampleSensitive().containsKey(entry.getKey())) {
//      throw new RuntimeException(
//          "Attempt to update sensitive sample data for key ("
//              + entry.getKey()
//              + ") using non-sensitive sample update message!");
//    } else {
//      caze.getSample().put(entry.getKey(), entry.getValue());
//    }
//  }

  private void validateOnlyNonSensitiveDataBeingUpdated(Case caze, Entry<String, String> entry) {
    if (caze.getSample().containsKey(entry.getKey())) {
      caze.getSample().put(entry.getKey(), entry.getValue());
    } else {
      throw new RuntimeException("Key (" + entry.getKey() + ") does not match an existing entry!");
    }
  }
}
