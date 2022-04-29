package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ScheduledTaskSchedulerTest {

  @Mock private ClusterLeaderManager clusterLeaderManager;
  @Mock private ScheduledTaskTriggerer scheduledTaskTriggerer;

  @InjectMocks ScheduledTaskScheduler underTest;

  @Test
  public void testSchedulerSchedules() {
    // Given
    when(clusterLeaderManager.isThisHostClusterLeader()).thenReturn(true);

    // When
    underTest.triggerScheduledTasks();

    // Then
    verify(scheduledTaskTriggerer, times(1)).triggerScheduledTasks();
  }
}
