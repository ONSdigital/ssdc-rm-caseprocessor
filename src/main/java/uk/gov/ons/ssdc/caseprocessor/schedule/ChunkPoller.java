package uk.gov.ons.ssdc.caseprocessor.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChunkPoller {
  private final ChunkProcessor chunkProcessor;

  public ChunkPoller(ChunkProcessor chunkProcessor) {
    this.chunkProcessor = chunkProcessor;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void processQueuedCases() {
    do {
      chunkProcessor.processChunk();
    } while (chunkProcessor.isThereWorkToDo()); // Don't go to sleep while there's work to do!
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void processQueuedFulfilments() {
    do {
      chunkProcessor.processFulfilmentChunk();
    } while (chunkProcessor
        .isThereFulfilmentWorkToDo()); // Don't go to sleep while there's work to do!
  }
}
