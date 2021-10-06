package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.NewCase;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.utils.CaseRefGenerator;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;

@MessageEndpoint
public class NewCaseReceiver {
  private final CaseRepository caseRepository;
  private final CaseService caseService;
  private final CollectionExerciseRepository collectionExerciseRepository;
  private final EventLogger eventLogger;

  @Value("${caserefgeneratorkey}")
  private byte[] caserefgeneratorkey;

  public NewCaseReceiver(
      CaseRepository caseRepository,
      CaseService caseService,
      CollectionExerciseRepository collectionExerciseRepository,
      EventLogger eventLogger) {
    this.caseRepository = caseRepository;
    this.caseService = caseService;
    this.collectionExerciseRepository = collectionExerciseRepository;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "newCaseInputChannel", adviceChain = "retryAdvice")
  public void receiveNewCase(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    NewCase newCasePayload = event.getPayload().getNewCase();

    if (caseRepository.existsById(newCasePayload.getCaseId())) {
      // Case already exists, so let's not overwrite it... swallow the message quietly
      return;
    }

    Optional<CollectionExercise> collexOpt =
        collectionExerciseRepository.findById(newCasePayload.getCollectionExerciseId());

    if (!collexOpt.isPresent()) {
      throw new RuntimeException(
          "Collection exercise '" + newCasePayload.getCollectionExerciseId() + "' not found");
    }

    CollectionExercise collex = collexOpt.get();

    ColumnValidator[] columnValidators = collex.getSurvey().getSampleValidationRules();

    Map<String, String> sampleRow = new HashMap<>();
    sampleRow.putAll(newCasePayload.getSample());
    sampleRow.putAll(newCasePayload.getSampleSensitive());

    for (ColumnValidator columnValidator : columnValidators) {
      Optional<String> columnValidationErrors = columnValidator.validateRow(sampleRow);
      if (columnValidationErrors.isPresent()) {
        throw new RuntimeException(columnValidationErrors.get());
      }
    }

    Case newCase = new Case();
    newCase.setId(newCasePayload.getCaseId());
    newCase.setCollectionExercise(collex);
    newCase.setSample(newCasePayload.getSample());
    newCase.setSampleSensitive(newCasePayload.getSampleSensitive());

    newCase = saveNewCaseAndStampCaseRef(newCase);
    caseService.emitCaseUpdate(
        newCase, event.getHeader().getCorrelationId(), event.getHeader().getOriginatingUser());

    eventLogger.logCaseEvent(newCase, "New case created", EventType.NEW_CASE, event, message);
  }

  private Case saveNewCaseAndStampCaseRef(Case caze) {
    caze = caseRepository.saveAndFlush(caze);
    caze.setCaseRef(
        CaseRefGenerator.getCaseRef(caze.getSecretSequenceNumber(), caserefgeneratorkey));
    caze = caseRepository.saveAndFlush(caze);

    return caze;
  }
}
