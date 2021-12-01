package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import java.util.Map;
import java.util.Map.Entry;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UpdateSampleSensitive;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.utils.SampleValidateHelper;
import uk.gov.ons.ssdc.common.model.entity.*;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;

@MessageEndpoint
public class UpdateSampleSensitiveReceiver {
  private final CaseService caseService;
  private final EventLogger eventLogger;

  public UpdateSampleSensitiveReceiver(CaseService caseService, EventLogger eventLogger) {
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  @ServiceActivator(inputChannel = "updateSampleSensitiveInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    UpdateSampleSensitive updateSampleSensitive = event.getPayload().getUpdateSampleSensitive();

    Case caze = caseService.getCaseAndLockForUpdate(updateSampleSensitive.getCaseId());

    for (Map.Entry<String, String> entry : updateSampleSensitive.getSampleSensitive().entrySet()) {
      String columnName = entry.getKey();
      String newValue = entry.getValue();
      // First, validate that only sensitive data that is defined is being attempted to be updated
      validateOnlySensitiveDataBeingUpdated(caze, entry);

      // Blanking out the sensitive PII data is allowed, for GDPR reasons
      if (entry.getValue().length() != 0) {

        // If the data is not being blanked, validate it according to rules
        for (ColumnValidator columnValidator :
            caze.getCollectionExercise().getSurvey().getSampleValidationRules()) {
          SampleValidateHelper.validateNewValue(columnName, newValue, columnValidator, EventType.UPDATE_SAMPLE_SENSITIVE);
        }
      }

      // Finally, update the cases sample sensitive blob with the validated value
      caze.getSampleSensitive().put(entry.getKey(), entry.getValue());
    }

    caseService.saveCaseAndEmitCaseUpdate(
        caze, event.getHeader().getCorrelationId(), event.getHeader().getOriginatingUser());

    eventLogger.logCaseEvent(
        caze, "Sensitive data updated", EventType.UPDATE_SAMPLE_SENSITIVE, event, message);
  }

  private void validateOnlySensitiveDataBeingUpdated(Case caze, String columnName) {
    //    TODO: Is this right?  Should it not be within the sampleSensitive definition?
//    Adding new data, if within sample def should be ok, if we have non mandatory fields?
    if (!caze.getSampleSensitive().containsKey(columnName)) {
      throw new RuntimeException("Column name (" + columnName + ") does not match an existing entry!");
    }
  }
}
