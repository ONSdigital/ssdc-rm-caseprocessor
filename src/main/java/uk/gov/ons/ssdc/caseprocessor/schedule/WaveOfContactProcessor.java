package uk.gov.ons.ssdc.caseprocessor.schedule;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.repository.WaveOfContactRepository;

@Component
public class WaveOfContactProcessor {
  private final CaseClassifier caseClassifier;
  private final WaveOfContactRepository waveOfContactRepository;

  public WaveOfContactProcessor(CaseClassifier caseClassifier, WaveOfContactRepository waveOfContactRepository) {
    this.caseClassifier = caseClassifier;
    this.waveOfContactRepository = waveOfContactRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every WoC
  public void createScheduledWaveOfContact(WaveOfContact triggeredWaveOfContact) {
    caseClassifier.enqueueCasesForWaveOfContact(triggeredWaveOfContact);
    triggeredWaveOfContact.setHasTriggered(true);
    waveOfContactRepository.save(triggeredWaveOfContact);
  }
}
