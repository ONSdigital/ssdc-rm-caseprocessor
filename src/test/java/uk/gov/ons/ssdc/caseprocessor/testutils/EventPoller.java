package uk.gov.ons.ssdc.caseprocessor.testutils;

import java.util.List;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;

@Component
@ActiveProfiles("test")
public class EventPoller {
  private final EventRepository eventRepository;

  public EventPoller(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @Retryable(
      value = {EventsNotFoundException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 2000))
  public List<Event> getEvents(int minExpectedEventCount) throws EventsNotFoundException {
    List<Event> events = eventRepository.findAll();

    if (events.size() < minExpectedEventCount) {
      throw new EventsNotFoundException(
          String.format(
              "Found: %d events, require at least: %d", events.size(), minExpectedEventCount));
    }

    return events;
  }
}
