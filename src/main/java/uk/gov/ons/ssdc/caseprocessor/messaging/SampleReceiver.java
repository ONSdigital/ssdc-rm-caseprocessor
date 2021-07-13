package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.EventHelper.createEventDTO;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.EventType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.utils.CaseRefGenerator;
import uk.gov.ons.ssdc.caseprocessor.utils.RedactHelper;

@MessageEndpoint
public class SampleReceiver {
  private final CaseRepository caseRepository;
  private final CaseService caseService;
  private final CollectionExerciseRepository collectionExerciseRepository;
  private final EventLogger eventLogger;

  @Value("${caserefgeneratorkey}")
  private byte[] caserefgeneratorkey;

  public SampleReceiver(
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
  @ServiceActivator(inputChannel = "sampleInputChannel")
  public void receiveSample(Message<Sample> message) {
    Sample sample = message.getPayload();
    if (caseRepository.existsById(sample.getCaseId())) {
      // Case already exists, so let's not overwrite it... swallow the message quietly
      return;
    }

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    Optional<CollectionExercise> collexOpt =
        collectionExerciseRepository.findById(sample.getCollectionExerciseId());

    if (!collexOpt.isPresent()) {
      throw new RuntimeException(
          "Collection exercise '" + sample.getCollectionExerciseId() + "' not found");
    }

    CollectionExercise collex = collexOpt.get();

    Case newCase = new Case();
    newCase.setId(sample.getCaseId());
    newCase.setCollectionExercise(collex);
    newCase.setSample(sample.getSample());
    newCase.setSampleSensitive(sample.getSampleSensitive());

    newCase = saveNewCaseAndStampCaseRef(newCase);
    caseService.emitCaseCreatedEvent(newCase);

    eventLogger.logCaseEvent(
        newCase,
        OffsetDateTime.now(),
        "Create case sample received",
        EventType.SAMPLE_LOADED,
        createEventDTO(EventTypeDTO.SAMPLE_LOADED),
        RedactHelper.redact(message.getPayload()),
        messageTimestamp);
  }

  private Case saveNewCaseAndStampCaseRef(Case caze) {
    caze = caseRepository.saveAndFlush(caze);
    caze.setCaseRef(
        CaseRefGenerator.getCaseRef(caze.getSecretSequenceNumber(), caserefgeneratorkey));
    caze = caseRepository.saveAndFlush(caze);

    return caze;
  }
}
