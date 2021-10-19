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

  @Transactional
  @ServiceActivator(inputChannel = "updateSampleSensitiveInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    UpdateSampleSensitive updateSampleSensitive = event.getPayload().getUpdateSampleSensitive();

    Case caze = caseService.getCaseByCaseId(updateSampleSensitive.getCaseId());

    for (Map.Entry<String, String> entry : updateSampleSensitive.getSampleSensitive().entrySet()) {
      // First, validate that only sensitive data that is defined is being attempted to be updated
      validateOnlySensitiveDataBeingUpdated(caze, entry);

      // Second, if the data is not being blanked, validate it according to rules
      if (entry.getValue().length() == 0) {
        // Blanking out the sensitive PII data is allowed, for GDPR reasons
        continue;
      }

      // Finally, validate the updated value according to the rules for the column
      for (ColumnValidator columnValidator :
          caze.getCollectionExercise().getSurvey().getSampleValidationRules()) {
        SampleValidateHelper.validateNewValue(entry, columnValidator, EventType.UPDATE_SAMPLE_SENSITIVE);
      }
    }

    caseService.saveCase(caze);

    eventLogger.logCaseEvent(
        caze, "Sensitive data updated", EventType.UPDATE_SAMPLE_SENSITIVE, event, message);
  }

  private void validateOnlySensitiveDataBeingUpdated(Case caze, Entry<String, String> entry) {
    if (caze.getSampleSensitive().containsKey(entry.getKey())) {
      caze.getSampleSensitive().put(entry.getKey(), entry.getValue());
    } else {
      throw new RuntimeException("Key (" + entry.getKey() + ") does not match an existing entry!");
    }
  }
}
