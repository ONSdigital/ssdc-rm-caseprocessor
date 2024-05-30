package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;

public class ChunkPollerTest {

  @Test
  public void testProcessQueuedCases() {
    // Given
    ChunkProcessor chunkProcessor = mock(ChunkProcessor.class);
    CaseToProcessRepository caseToProcessRepository = mock(CaseToProcessRepository.class);
    ChunkPoller underTest = new ChunkPoller(chunkProcessor, caseToProcessRepository);

    // When
    underTest.processQueuedCases();

    // Then
    verify(chunkProcessor).processChunk();
    verify(chunkProcessor).isThereWorkToDo();
  }
}
