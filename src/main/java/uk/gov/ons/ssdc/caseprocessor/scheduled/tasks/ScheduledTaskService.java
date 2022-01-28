package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.Event;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@Component
public class ScheduledTaskService {

  private final ScheduledTaskRepository scheduledTaskRepository;
  private final UacService uacService;

  public ScheduledTaskService(
      ScheduledTaskRepository scheduledTaskRepository, UacService uacService) {
    this.scheduledTaskRepository = scheduledTaskRepository;
    this.uacService = uacService;
  }

  // Add data like Event to ScheduledTask
  public void updateScheculedTaskSentEvent(ScheduledTask scheduledTask, Event event, UacQidLink uacQidLink) {

    // The perils of STATE, would we just log this against the case / Scheduled Task anyway.
    // It feels like a big Error to be calling this function at the moment if not in this STATE
    // Not required for this POC, but shows potential issues with approach.
    // Is a coded table / Set of STATES and matching conditions a good idea, or madness.
/*


                                                                          ▒▒
                                                                        ░░▒▒
                                                                        ▒▒▒▒▒▒
                                                                      ░░▒▒▒▒▒▒
                                                                      ▒▒▒▒▒▒▒▒▒▒
                                                                    ░░▒▒▒▒▒▒▒▒▒▒
                                                                    ▒▒▒▒▒▒▒▒▒▒▒▒▒▒
                                                                  ░░▒▒▒▒▒▒▒▒▒▒▒▒▒▒
                                                                  ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
                                                                ░░▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
                                                                ▒▒▒▒▒▒▒▒▒▒  ▒▒▒▒▒▒▒▒▒▒
                                                              ░░▒▒▒▒▒▒▒▒░░  ▒▒▒▒▒▒▒▒▒▒
                                                              ▒▒▒▒▒▒▒▒▒▒      ▒▒▒▒▒▒▒▒▒▒
                                                            ░░▒▒▒▒▒▒▒▒░░      ▒▒▒▒▒▒▒▒▒▒
                                                            ▒▒▒▒▒▒▒▒▒▒          ▒▒▒▒▒▒▒▒▒▒
                                                          ░░▒▒▒▒▒▒▒▒░░          ▒▒▒▒▒▒▒▒▒▒
                                                          ▒▒▒▒▒▒▒▒▒▒              ▒▒▒▒▒▒▒▒▒▒
                                                        ░░▒▒▒▒▒▒▒▒░░              ▒▒▒▒▒▒▒▒▒▒
                                                        ▒▒▒▒▒▒▒▒▒▒                  ▒▒▒▒▒▒▒▒▒▒
                                                      ░░▒▒▒▒▒▒▒▒░░                  ▒▒▒▒▒▒▒▒▒▒
                                                      ▒▒▒▒▒▒▒▒▒▒                      ▒▒▒▒▒▒▒▒▒▒
                                                    ░░▒▒▒▒▒▒▒▒░░                      ▒▒▒▒▒▒▒▒▒▒
                                                    ▒▒▒▒▒▒▒▒▒▒                          ▒▒▒▒▒▒▒▒▒▒
                                                  ░░▒▒▒▒▒▒▒▒░░                          ▒▒▒▒▒▒▒▒▒▒
                                                  ▒▒▒▒▒▒▒▒▒▒                              ▒▒▒▒▒▒▒▒▒▒
                                                ░░▒▒▒▒▒▒▒▒░░                              ▒▒▒▒▒▒▒▒▒▒
                                                ▒▒▒▒▒▒▒▒▒▒          ░░████████████▒▒        ▒▒▒▒▒▒▒▒▒▒
                                              ░░▒▒▒▒▒▒▒▒░░          ████████████████        ▒▒▒▒▒▒▒▒▒▒
                                              ▒▒▒▒▒▒▒▒▒▒          ████████████████████        ▒▒▒▒▒▒▒▒▒▒
                                            ░░▒▒▒▒▒▒▒▒░░          ▓▓██████████████████        ▒▒▒▒▒▒▒▒▒▒
                                            ▒▒▒▒▒▒▒▒▒▒            ████████████████████          ▒▒▒▒▒▒▒▒▒▒
                                          ░░▒▒▒▒▒▒▒▒░░            ▒▒██████████████████          ▒▒▒▒▒▒▒▒▒▒
                                          ▒▒▒▒▒▒▒▒▒▒                ████████████████              ▒▒▒▒▒▒▒▒▒▒
                                        ░░▒▒▒▒▒▒▒▒▒▒                ████████████████              ▒▒▒▒▒▒▒▒▒▒
                                        ▒▒▒▒▒▒▒▒▒▒                  ████████████████                ▒▒▒▒▒▒▒▒▒▒
                                      ░░▒▒▒▒▒▒▒▒▒▒                  ████████████████                ▒▒▒▒▒▒▒▒▒▒
                                      ▒▒▒▒▒▒▒▒▒▒                    ██████████████▓▓                  ▒▒▒▒▒▒▒▒▒▒
                                    ░░▒▒▒▒▒▒▒▒░░                    ▒▒██████████████                  ▒▒▒▒▒▒▒▒▒▒
                                    ▒▒▒▒▒▒▒▒▒▒                        ████████████                      ▒▒▒▒▒▒▒▒▒▒
                                  ░░▒▒▒▒▒▒▒▒░░                        ████████████                      ▒▒▒▒▒▒▒▒▒▒
                                  ▒▒▒▒▒▒▒▒▒▒                          ████████████                        ▒▒▒▒▒▒▒▒▒▒
                                ░░▒▒▒▒▒▒▒▒░░                          ████████████                        ▒▒▒▒▒▒▒▒▒▒
                                ▒▒▒▒▒▒▒▒▒▒                            ▓▓██████████                          ▒▒▒▒▒▒▒▒▒▒
                              ░░▒▒▒▒▒▒▒▒░░                            ████████████                          ▒▒▒▒▒▒▒▒▒▒
                              ▒▒▒▒▒▒▒▒▒▒                                ██████████                            ▒▒▒▒▒▒▒▒▒▒
                            ░░▒▒▒▒▒▒▒▒░░                                ████████                              ▒▒▒▒▒▒▒▒▒▒
                            ▒▒▒▒▒▒▒▒▒▒                                  ████████                                ▒▒▒▒▒▒▒▒▒▒
                          ░░▒▒▒▒▒▒▒▒░░                                  ████████                                ▒▒▒▒▒▒▒▒▒▒
                          ▒▒▒▒▒▒▒▒▒▒                                    ████████                                  ▒▒▒▒▒▒▒▒▒▒
                        ░░▒▒▒▒▒▒▒▒░░                                                                              ▒▒▒▒▒▒▒▒▒▒
                        ▒▒▒▒▒▒▒▒▒▒                                                                                  ▒▒▒▒▒▒▒▒▒▒
                      ░░▒▒▒▒▒▒▒▒░░                                                                                  ▒▒▒▒▒▒▒▒▒▒
                      ▒▒▒▒▒▒▒▒▒▒                                                                                      ▒▒▒▒▒▒▒▒▒▒
                    ░░▒▒▒▒▒▒▒▒░░                                      ▒▒██████████                                    ▒▒▒▒▒▒▒▒▒▒
                    ▒▒▒▒▒▒▒▒▒▒                                      ▒▒████████████▒▒                                    ▒▒▒▒▒▒▒▒▒▒
                  ░░▒▒▒▒▒▒▒▒░░                                      ████████████████                                    ▒▒▒▒▒▒▒▒▒▒
                  ▒▒▒▒▒▒▒▒▒▒                                        ████████████████                                      ▒▒▒▒▒▒▒▒▒▒
                ░░▒▒▒▒▒▒▒▒░░                                        ████████████████                                      ▒▒▒▒▒▒▒▒▒▒
                ▒▒▒▒▒▒▒▒▒▒                                          ████████████████                                        ▒▒▒▒▒▒▒▒▒▒
              ░░▒▒▒▒▒▒▒▒░░                                          ████████████████                                        ▒▒▒▒▒▒▒▒▒▒
              ▒▒▒▒▒▒▒▒▒▒                                              ████████████                                            ▒▒▒▒▒▒▒▒▒▒
            ░░▒▒▒▒▒▒▒▒░░                                                ████████                                              ▒▒▒▒▒▒▒▒▒▒
            ▒▒▒▒▒▒▒▒▒▒                                                                                                          ▒▒▒▒▒▒▒▒▒▒
          ░░▒▒▒▒▒▒▒▒░░                                                                                                          ▒▒▒▒▒▒▒▒▒▒
          ▒▒▒▒▒▒▒▒▒▒                                                                                                              ▒▒▒▒▒▒▒▒▒▒
        ░░▒▒▒▒▒▒▒▒░░                                                                                                              ▒▒▒▒▒▒▒▒▒▒
        ▒▒▒▒▒▒▒▒▒▒                                                                                                                  ▒▒▒▒▒▒▒▒▒▒
      ░░▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
      ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
    ░░▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
    ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
  ░░▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒



 */
    if (scheduledTask.getActionState() != ScheduledTaskState.IN_FULFILMENT) {
      throw new RuntimeException(
          String.format(
              "ScheduledTask with ID Should be in State %s, is in State %s",
              ScheduledTaskState.IN_FULFILMENT, scheduledTask.getActionState()));
    }

    // A few potential edge cases....
    if (scheduledTask.getSentEvent() != null) {
      throw new RuntimeException(
          String.format(
              "Already a sent Event recorded for this scheduledTask?: %s", scheduledTask.getId()));
    }

    scheduledTask.setSentEvent(event);

    // This might be null, for example a Reminder at start of ResponsePeriod.  Doesn't matter
    scheduledTask.setUacQidLink(uacQidLink);

    // Might want to check State is currently as expected?
    // We might be able to go without this, e.g. UACQidLink suggests it needs a receipting event.
    // But in another way a State is easier to query, and easier for anyone viewing the Task
    // State is a pain and has pitfalls.  None State also has pitfalls.
    if (scheduledTask.isReceiptRequiredForCompletion()) {
      scheduledTask.setActionState(ScheduledTaskState.SENT);
    } else {
      scheduledTask.setActionState(ScheduledTaskState.COMPLETED);
    }

    scheduledTaskRepository.saveAndFlush(scheduledTask);
  }

  public ScheduledTask getById(UUID scheduledTaskId) {
    Optional<ScheduledTask> scheduledTaskResult = scheduledTaskRepository.findById(scheduledTaskId);

    if (scheduledTaskResult.isEmpty()) {
      throw new RuntimeException(
          String.format("ScheduledTask with ID '%s' not found", scheduledTaskId));
    }

    return scheduledTaskResult.get();
  }
}
