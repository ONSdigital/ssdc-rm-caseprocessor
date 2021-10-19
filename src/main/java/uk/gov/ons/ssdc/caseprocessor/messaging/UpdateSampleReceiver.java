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
import uk.gov.ons.ssdc.caseprocessor.utils.SampleValidateHelper;
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

    for (Map.Entry<String, String> entry : updateSample.getSample().entrySet()) {
      // First, validate that only sample data that is defined is being attempted to be updated
      validateOnlySampleDataBeingUpdated(caze, entry);

      // Second, do not allow the data to be blanked
      if (entry.getValue().length() == 0) {
        throw new RuntimeException(
            "Cannot update sample data to blank (" + entry.getKey() + ")");
      }

      // Finally, validate the updated value according to the rules for the column
      for (ColumnValidator columnValidator :
          caze.getCollectionExercise().getSurvey().getSampleValidationRules()) {
        SampleValidateHelper.validateNewValue(entry, columnValidator, EventType.UPDATE_SAMPLE);
      }
    }

    caseService.saveCase(caze);

    eventLogger.logCaseEvent(
        caze, "Sample data updated", EventType.UPDATE_SAMPLE, event, message);
  }

  private void validateOnlySampleDataBeingUpdated(Case caze, Entry<String, String> entry) {
    if (caze.getSample().containsKey(entry.getKey())) {
      caze.getSample().put(entry.getKey(), entry.getValue());
    } else {
      throw new RuntimeException("Key (" + entry.getKey() + ") does not match an existing entry!");
    }
  }
}
