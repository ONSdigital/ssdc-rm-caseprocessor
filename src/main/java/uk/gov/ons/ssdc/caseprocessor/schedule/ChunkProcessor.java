package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.service.CaseToProcessProcessor;
import uk.gov.ons.ssdc.caseprocessor.service.PrintProcessor;
import uk.gov.ons.ssdc.common.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.common.model.entity.FulfilmentToProcess;

@Component
public class ChunkProcessor {
  private final CaseToProcessRepository caseToProcessRepository;
  private final CaseToProcessProcessor caseToProcessProcessor;
  private final FulfilmentToProcessRepository fulfilmentToProcessRepository;
  private final PrintProcessor printProcessor;

  @Value("${scheduler.chunksize}")
  private int chunkSize;

  public ChunkProcessor(
      CaseToProcessRepository caseToProcessRepository,
      CaseToProcessProcessor caseToProcessProcessor,
      FulfilmentToProcessRepository fulfilmentToProcessRepository,
      PrintProcessor printProcessor) {
    this.caseToProcessRepository = caseToProcessRepository;
    this.caseToProcessProcessor = caseToProcessProcessor;
    this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
    this.printProcessor = printProcessor;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every chunk
  public void processChunk() {
    try (Stream<CaseToProcess> cases = caseToProcessRepository.findChunkToProcess(chunkSize)) {
      List<CaseToProcess> caseToProcessToDelete = new LinkedList<>();

      cases.forEach(
          caseToProcess -> {
            caseToProcessProcessor.process(caseToProcess);
            caseToProcessToDelete.add(caseToProcess);
          });

      caseToProcessRepository.deleteAllInBatch(caseToProcessToDelete);
    }
  }

  @Transactional
  public boolean isThereWorkToDo() {
    return caseToProcessRepository.count() > 0;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every chunk
  public void processFulfilmentChunk() {
    try (Stream<FulfilmentToProcess> cases =
        fulfilmentToProcessRepository.findChunkToProcess(chunkSize)) {
      List<FulfilmentToProcess> fulfilmentToProcessToDelete = new LinkedList<>();

      cases.forEach(
          fulfilmentToProcess -> {
            printProcessor.process(fulfilmentToProcess);
            fulfilmentToProcessToDelete.add(fulfilmentToProcess);
          });

      fulfilmentToProcessRepository.deleteAllInBatch(fulfilmentToProcessToDelete);
    }
  }

  @Transactional
  public boolean isThereFulfilmentWorkToDo() {
    return caseToProcessRepository.count() > 0;
  }
}
