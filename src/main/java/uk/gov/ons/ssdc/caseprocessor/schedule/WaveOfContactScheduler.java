package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WaveOfContactScheduler {
  private static final Logger log = LoggerFactory.getLogger(WaveOfContactScheduler.class);
  private final WaveOfContactTriggerer waveOfContactTriggerer;

  public WaveOfContactScheduler(WaveOfContactTriggerer waveOfContactTriggerer) {
    this.waveOfContactTriggerer = waveOfContactTriggerer;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void triggerActionRules() {
    try {
      waveOfContactTriggerer.triggerActionRules();
    } catch (Exception e) {
      log.error("Unexpected exception while processing Action Rules", e);
      throw e;
    }
  }
}
