package uk.gov.ons.ssdc.caseprocessor.messaging;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.CaseRefGenerator;

@MessageEndpoint
public class SampleReceiver {
  private final CaseRepository caseRepository;
  private final CollectionExerciseRepository collectionExerciseRepository;

  @Value("${caserefgeneratorkey}")
  private byte[] caserefgeneratorkey;

  public SampleReceiver(
      CaseRepository caseRepository, CollectionExerciseRepository collectionExerciseRepository) {
    this.caseRepository = caseRepository;
    this.collectionExerciseRepository = collectionExerciseRepository;
  }

  @ServiceActivator(inputChannel = "sampleInputChannel")
  public void receiveSample(Sample sample) {
    if (caseRepository.existsById(sample.getCaseId())) {
      // Case already exists, so let's not overwrite it... swallow the message quietly
      return;
    }

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

    newCase = saveNewCaseAndStampCaseRef(newCase);

    System.out.println("Added new case to DB with case ref: " + newCase.getCaseRef());
  }

  private Case saveNewCaseAndStampCaseRef(Case caze) {
    caze = caseRepository.saveAndFlush(caze);
    caze.setCaseRef(
        CaseRefGenerator.getCaseRef(caze.getSecretSequenceNumber(), caserefgeneratorkey));
    caze = caseRepository.saveAndFlush(caze);

    return caze;
  }
}
