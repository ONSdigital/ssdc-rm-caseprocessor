package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.repository.WaveOfContactRepository;

@Component
public class WaveOfContactTriggerer {
  private static final Logger log = LoggerFactory.getLogger(WaveOfContactTriggerer.class);
  private final WaveOfContactRepository waveOfContactRepository;
  private final WaveOfContactProcessor waveOfContactProcessor;

  public WaveOfContactTriggerer(
      WaveOfContactRepository waveOfContactRepository,
      WaveOfContactProcessor waveOfContactProcessor) {
    this.waveOfContactRepository = waveOfContactRepository;
    this.waveOfContactProcessor = waveOfContactProcessor;
  }

  @Transactional
  public void triggerActionRules() {
    List<WaveOfContact> triggeredWaveOfContacts =
        waveOfContactRepository.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            OffsetDateTime.now());

    for (WaveOfContact triggeredWaveOfContact : triggeredWaveOfContacts) {
      try {
        log.with("id", triggeredWaveOfContact.getId()).info("Action rule triggered");
        waveOfContactProcessor.createScheduledWaveOfContact(triggeredWaveOfContact);
      } catch (Exception e) {
        log.with("id", triggeredWaveOfContact.getId())
            .error("Unexpected error while executing action rule - is classifier valid SQL?", e);
      }
    }
  }
}
