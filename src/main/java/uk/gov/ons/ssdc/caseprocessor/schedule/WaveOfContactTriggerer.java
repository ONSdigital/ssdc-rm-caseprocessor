package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

  private String hostName = InetAddress.getLocalHost().getHostName();

  public WaveOfContactTriggerer(
      WaveOfContactRepository waveOfContactRepository,
      WaveOfContactProcessor waveOfContactProcessor)
      throws UnknownHostException {
    this.waveOfContactRepository = waveOfContactRepository;
    this.waveOfContactProcessor = waveOfContactProcessor;
  }

  @Transactional
  public void triggerWaveOfContact() {
    List<WaveOfContact> triggeredWaveOfContacts =
        waveOfContactRepository.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            OffsetDateTime.now());

    for (WaveOfContact triggeredWaveOfContact : triggeredWaveOfContacts) {
      try {
        log.with("hostName", hostName)
            .with("id", triggeredWaveOfContact.getId())
            .info("Wave of contact triggered");
        waveOfContactProcessor.createScheduledWaveOfContact(triggeredWaveOfContact);
      } catch (Exception e) {
        log.with("id", triggeredWaveOfContact.getId())
            .error(
                "Unexpected error while executing wave of contact - is classifier valid SQL?", e);
      }
    }
  }
}
