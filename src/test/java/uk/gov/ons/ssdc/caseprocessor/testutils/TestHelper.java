package uk.gov.ons.ssdc.caseprocessor.testutils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Event;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;

@Component
@ActiveProfiles("test")
@EnableRetry
public class TestHelper {

  @Autowired CaseRepository caseRepository;
  @Autowired EventRepository eventRepository;

  @Retryable(
      value = {java.io.IOException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 500))
  public Case pollUntilCaseExists(UUID caseId) throws IOException {

    Optional<Case> caseOpt = caseRepository.findById(caseId);

    if (caseOpt.isPresent()) {
      return caseOpt.get();
    }

    throw new IOException("case Not found");
  }

  @Retryable(
      value = {java.io.IOException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 500))
  public List<Event> pollUntilEventsExist() throws IOException {

    List<Event> events = eventRepository.findAll();

    if (events.size() > 0) {
      return events;
    }

    throw new IOException("Events Not found");
  }
}
