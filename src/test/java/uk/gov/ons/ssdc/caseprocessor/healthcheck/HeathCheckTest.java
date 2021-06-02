package uk.gov.ons.ssdc.caseprocessor.healthcheck;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ssdc.caseprocessor.schedule.ClusterLeaderManager;

@RunWith(MockitoJUnitRunner.class)
public class HeathCheckTest {
  @Mock private ClusterLeaderManager clusterLeaderManager;

  @InjectMocks HeathCheck underTest;

  @Test
  public void testHappyPath() {
    // Given
    ReflectionTestUtils.setField(underTest, "fileName", "/tmp/" + UUID.randomUUID());

    // When
    underTest.updateFileWithCurrentTimestamp();

    // Then
    verify(clusterLeaderManager).leaderKeepAlive();
  }
}
