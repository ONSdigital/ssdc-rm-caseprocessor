package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.rasrm.constants.RasRmConstants.BUSINESS_SAMPLE_DEFINITION_URL_SUFFIX;
import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import uk.gov.ons.ssdc.caseprocessor.rasrm.service.RasRmCaseNotificationEnrichmentService;
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
  private final RasRmCaseNotificationEnrichmentService rasRmNewBusinessCaseEnricher;

  @Value("${caserefgeneratorkey}")
  private byte[] caserefgeneratorkey;

  public NewCaseReceiver(
      CaseRepository caseRepository,
      CaseService caseService,
      CollectionExerciseRepository collectionExerciseRepository,
      EventLogger eventLogger,
      RasRmCaseNotificationEnrichmentService rasRmNewBusinessCaseEnricher) {
    this.caseRepository = caseRepository;
    this.caseService = caseService;
    this.collectionExerciseRepository = collectionExerciseRepository;
    this.eventLogger = eventLogger;
    this.rasRmNewBusinessCaseEnricher = rasRmNewBusinessCaseEnricher;
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

    CollectionExercise collex =
        collectionExerciseRepository
            .findById(newCasePayload.getCollectionExerciseId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Collection exercise '"
                            + newCasePayload.getCollectionExerciseId()
                            + "' not found"));

    ColumnValidator[] columnValidators = collex.getSurvey().getSampleValidationRules();
    checkNewSampleWithinSampleDefinition(columnValidators, newCasePayload);
    checkNewSensitiveWithinSampleSensitiveDefinition(columnValidators, newCasePayload);

    validateNewCase(newCasePayload, columnValidators);

    Map<String, String> sample = newCasePayload.getSample();

    if (collex
        .getSurvey()
        .getSampleDefinitionUrl()
        .endsWith(BUSINESS_SAMPLE_DEFINITION_URL_SUFFIX)) {
      sample =
          rasRmNewBusinessCaseEnricher.notifyRasRmAndEnrichSample(sample, collex.getMetadata());
    }

    Case newCase = new Case();
    newCase.setId(newCasePayload.getCaseId());
    newCase.setCollectionExercise(collex);
    newCase.setSample(sample);
    newCase.setSampleSensitive(newCasePayload.getSampleSensitive());

    newCase = saveNewCaseAndStampCaseRef(newCase);
    caseService.emitCaseUpdate(
        newCase, event.getHeader().getCorrelationId(), event.getHeader().getOriginatingUser());

    eventLogger.logCaseEvent(newCase, "New case created", EventType.NEW_CASE, event, message);
  }

  private Set<String> checkNewSensitiveWithinSampleSensitiveDefinition(
      ColumnValidator[] columnValidators, NewCase newCasePayload) {
    Set<String> sensitiveColumns =
        Arrays.stream(columnValidators)
            .filter(ColumnValidator::isSensitive)
            .map(ColumnValidator::getColumnName)
            .collect(Collectors.toSet());
    if (!sensitiveColumns.containsAll(newCasePayload.getSampleSensitive().keySet())) {
      throw new RuntimeException(
          "Attempt to send sensitive data to RM which was not part of defined sample");
    }

    return sensitiveColumns;
  }

  private void checkNewSampleWithinSampleDefinition(
      ColumnValidator[] columnValidators, NewCase newCasePayload) {
    Set<String> nonSensitiveColumns =
        Arrays.stream(columnValidators)
            .filter(columnValidator -> !columnValidator.isSensitive())
            .map(ColumnValidator::getColumnName)
            .collect(Collectors.toSet());
    if (!nonSensitiveColumns.containsAll(newCasePayload.getSample().keySet())) {
      throw new RuntimeException("Attempt to send data to RM which was not part of defined sample");
    }
  }

  private void validateNewCase(NewCase newCasePayload, ColumnValidator[] columnValidators) {
    List<String> validationErrors = new ArrayList<>();

    for (ColumnValidator columnValidator : columnValidators) {
      if (columnValidator.isSensitive()) {
        columnValidator
            .validateRow(newCasePayload.getSampleSensitive(), true)
            .ifPresent(validationErrors::add);
      } else {
        columnValidator
            .validateRow(newCasePayload.getSample(), true)
            .ifPresent(validationErrors::add);
      }
    }

    if (!validationErrors.isEmpty()) {
      throw new RuntimeException(
          "NEW_CASE event: "
              + validationErrors.stream().collect(Collectors.joining(System.lineSeparator())));
    }
  }

  private Case saveNewCaseAndStampCaseRef(Case caze) {
    Case newCase = caseRepository.saveAndFlush(caze);
    newCase.setCaseRef(
        CaseRefGenerator.getCaseRef(newCase.getSecretSequenceNumber(), caserefgeneratorkey));
    return caseRepository.saveAndFlush(newCase);
  }
}
