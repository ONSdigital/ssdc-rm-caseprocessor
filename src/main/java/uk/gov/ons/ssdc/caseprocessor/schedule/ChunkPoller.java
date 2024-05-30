package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;

@Component
public class ChunkPoller {
  private final ChunkProcessor chunkProcessor;

  private static final Logger log = LoggerFactory.getLogger(ActionRuleTriggerer.class);

  private final CaseToProcessRepository caseToProcessRepository;

  private Set<String> scheduledActionRules = new HashSet<>(); // could/should be converted to UUIDS

  public ChunkPoller(
      ChunkProcessor chunkProcessor, CaseToProcessRepository caseToProcessRepository) {
    this.chunkProcessor = chunkProcessor;
    this.caseToProcessRepository = caseToProcessRepository;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void processQueuedCases() {
    do {
      getScheduledActionRules();
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

  private void getScheduledActionRules() {
    Set<String> newScheduledActionRules = caseToProcessRepository.getActionRuleIds();

    Set<String> finishedActionRules =
        scheduledActionRules; // not a fan of this - might be a better approach
    finishedActionRules.removeAll(newScheduledActionRules);
    markActionRulesAsFinished(finishedActionRules);
    scheduledActionRules = newScheduledActionRules;
  }

  private void markActionRulesAsFinished(Set<String> finishedActionRules) {
    for (String finishedActionRule : finishedActionRules) {
      log.atInfo().setMessage("Action rule Completed").addKeyValue("Id", finishedActionRule).log();
    }
  }
}
