package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ssdc.caseprocessor.model.entity.FulfilmentNextTrigger;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentNextTriggerRepository;

@RunWith(MockitoJUnitRunner.class)
public class FulfilmentSchedulerTest {
  @Mock
  private ClusterLeaderManager clusterLeaderManager;
  @Mock
  private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;
  @Mock
  private FulfilmentProcessor fulfilmentProcessor;

  @InjectMocks
  private FulfilmentScheduler underTest;

  @Test
  public void testScheduleFulfilments() {
    // Given
    FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
    OffsetDateTime dateTimeNow = OffsetDateTime.now();
    fulfilmentNextTrigger.setTriggerDateTime(dateTimeNow);

    when(clusterLeaderManager.isThisHostClusterLeader()).thenReturn(true);
    when(fulfilmentNextTriggerRepository.findByTriggerDateTimeBefore(any(OffsetDateTime.class))).thenReturn(
        Optional.of(fulfilmentNextTrigger));

    // When
    underTest.scheduleFulfilments();

    // Then
    verify(fulfilmentProcessor).addFulfilmentBatchIdAndQuantity();
    verify(fulfilmentNextTriggerRepository).saveAndFlush(fulfilmentNextTrigger);

    assertThat(fulfilmentNextTrigger.getTriggerDateTime()).isEqualTo(dateTimeNow.plusDays(1));
  }

  @Test
  public void testScheduleFulfilmentsNothingToDo() {
    // Given
    FulfilmentNextTrigger fulfilmentNextTrigger = new FulfilmentNextTrigger();
    OffsetDateTime dateTimeNow = OffsetDateTime.now();
    fulfilmentNextTrigger.setTriggerDateTime(dateTimeNow);

    when(clusterLeaderManager.isThisHostClusterLeader()).thenReturn(true);
    when(fulfilmentNextTriggerRepository.findByTriggerDateTimeBefore(any(OffsetDateTime.class))).thenReturn(
        Optional.empty());

    // When
    underTest.scheduleFulfilments();

    // Then
    verifyNoInteractions(fulfilmentProcessor);
    verify(fulfilmentNextTriggerRepository).findByTriggerDateTimeBefore(any(OffsetDateTime.class));
    verifyNoMoreInteractions(fulfilmentNextTriggerRepository);
  }
}