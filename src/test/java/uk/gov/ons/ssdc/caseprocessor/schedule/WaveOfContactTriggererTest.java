package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.repository.WaveOfContactRepository;

public class WaveOfContactTriggererTest {
  private final WaveOfContactRepository waveOfContactRepository =
      mock(WaveOfContactRepository.class);
  private final WaveOfContactProcessor actionRuleProcessor = mock(WaveOfContactProcessor.class);

  @Test
  public void testTriggerActionRules() {
    // Given
    WaveOfContact waveOfContact = new WaveOfContact();
    when(waveOfContactRepository.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            any(OffsetDateTime.class)))
        .thenReturn(Collections.singletonList(waveOfContact));

    // When
    WaveOfContactTriggerer underTest =
        new WaveOfContactTriggerer(waveOfContactRepository, actionRuleProcessor);
    underTest.triggerWaveOfContact();

    // Then
    verify(actionRuleProcessor).createScheduledWaveOfContact(eq(waveOfContact));
  }

  @Test
  public void testTriggerMultipleActionRules() {
    // Given
    List<WaveOfContact> actionRules = new ArrayList<>(50);
    for (int i = 0; i < 50; i++) {
      actionRules.add(new WaveOfContact());
    }

    when(waveOfContactRepository.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            any(OffsetDateTime.class)))
        .thenReturn(actionRules);

    // When
    WaveOfContactTriggerer underTest =
        new WaveOfContactTriggerer(waveOfContactRepository, actionRuleProcessor);
    underTest.triggerWaveOfContact();

    // Then
    verify(actionRuleProcessor, times(50)).createScheduledWaveOfContact(any(WaveOfContact.class));
  }
}
