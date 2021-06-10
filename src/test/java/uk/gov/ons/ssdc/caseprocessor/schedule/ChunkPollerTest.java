package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class ChunkPollerTest {

  @Test
  public void testProcessQueuedCases() {
    // Given
    ChunkProcessor chunkProcessor = mock(ChunkProcessor.class);
    ChunkPoller underTest = new ChunkPoller(chunkProcessor);

    // When
    underTest.processQueuedCases();

    // Then
    verify(chunkProcessor).processChunk();
    verify(chunkProcessor).isThereWorkToDo();
  }
}
