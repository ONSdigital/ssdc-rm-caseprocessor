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

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  @ServiceActivator(inputChannel = "updateSampleInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    UpdateSample updateSample = event.getPayload().getUpdateSample();

    Case caze = caseService.getCaseAndLockForUpdate(updateSample.getCaseId());

    for (Map.Entry<String, String> entry : updateSample.getSample().entrySet()) {
      String columnName = entry.getKey();
      String newValue = entry.getValue();
      // Validate that only existing sample data is being attempted to be updated
      validateOnlyExistingSampleDataBeingUpdated(caze, columnName);

      // Validate the updated value according to the rules for the column
      for (ColumnValidator columnValidator :
          caze.getCollectionExercise().getSurvey().getSampleValidationRules()) {
        SampleValidateHelper.validateNewValue(columnName, newValue, columnValidator, EventType.UPDATE_SAMPLE);
      }

      caze.getSample().put(entry.getKey(), entry.getValue());
    }

    caseService.saveCaseAndEmitCaseUpdate(
        caze, event.getHeader().getCorrelationId(), event.getHeader().getOriginatingUser());

    eventLogger.logCaseEvent(caze, "Sample data updated", EventType.UPDATE_SAMPLE, event, message);
  }

  private void validateOnlyExistingSampleDataBeingUpdated(Case caze, String columnName) {
//    TODO: Is this right?  Should it not be within the sample definition?
//    Adding new data, if within sample def should be ok, if we have non mandatory fields?
    if (!caze.getSample().containsKey(columnName)) {
      throw new RuntimeException(
          "Column name (" + columnName + ") does not exist in current sample");
    }
  }
}
