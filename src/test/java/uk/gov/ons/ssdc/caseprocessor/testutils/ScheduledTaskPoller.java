package uk.gov.ons.ssdc.caseprocessor.testutils;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;

@Component
@ActiveProfiles("test")
public class ScheduledTaskPoller {
  private final ScheduledTaskRepository scheduledTaskRepository;

  public ScheduledTaskPoller(ScheduledTaskRepository scheduledTaskRepository) {
    this.scheduledTaskRepository = scheduledTaskRepository;
  }

  @Retryable(
      value = {ScheduledTasksNotProcessedException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 500))
  public void waitUntilScheduledTasksProcessed() throws ScheduledTasksNotProcessedException {
    if (scheduledTaskRepository.findAll().size() > 0) {
      throw new ScheduledTasksNotProcessedException(
          "Expected 0 scheduled tasks to not be processed");
    }
  }
}
