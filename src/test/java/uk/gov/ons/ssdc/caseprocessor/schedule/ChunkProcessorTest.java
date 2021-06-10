package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseToProcessProcessor;

@RunWith(MockitoJUnitRunner.class)
public class ChunkProcessorTest {
  @Mock private CaseToProcessRepository caseToProcessRepository;

  @Mock private CaseToProcessProcessor caseToProcessProcessor;

  @InjectMocks private ChunkProcessor underTest;

  @Value("${scheduler.chunksize}")
  private int chunkSize;

  @Test
  public void testProcessChunk() {
    // Given
    CaseToProcess caseToProcess = new CaseToProcess();
    List<CaseToProcess> caseToProcessList = new LinkedList<>();
    caseToProcessList.add(caseToProcess);
    when(caseToProcessRepository.findChunkToProcess(anyInt()))
        .thenReturn(caseToProcessList.stream());

    // When
    underTest.processChunk();

    // Then
    verify(caseToProcessRepository).findChunkToProcess(eq(chunkSize));
    verify(caseToProcessProcessor).process(eq(caseToProcess));
    verify(caseToProcessRepository).delete(eq(caseToProcess));
  }

  @Test
  public void testIsThereWorkToDoNoThereIsNot() {
    // Given
    when(caseToProcessRepository.count()).thenReturn(0L);

    // When
    boolean actualResult = underTest.isThereWorkToDo();

    // Then
    verify(caseToProcessRepository).count();
    assertThat(actualResult).isFalse();
  }

  @Test
  public void testIsThereWorkToDoYesThereIs() {
    // Given
    when(caseToProcessRepository.count()).thenReturn(666L);

    // When
    boolean actualResult = underTest.isThereWorkToDo();

    // Then
    verify(caseToProcessRepository).count();
    assertThat(actualResult).isTrue();
  }
}
