package uk.gov.ons.ssdc.caseprocessor.schedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.FulfilmentService;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType.ACTION_WITH_PACKCODE;

@ExtendWith(MockitoExtension.class)
public class ScheduledTaskProcessorTest {

  @Mock private FulfilmentService fulfilmentService;
  @Mock private CaseService caseService;

  @InjectMocks private ScheduledTaskProcessor underTest;

  @Test
  public void testProcessActionWithPackCodeIsCalled() {
    // Given
    ScheduledTask scheduledTask = new ScheduledTask();
    scheduledTask.setId(UUID.randomUUID());
    scheduledTask.setScheduledTaskType(ACTION_WITH_PACKCODE);

    Case caze = new Case();
    when(caseService.getCase(any())).thenReturn(caze);

    // When
    underTest.process(scheduledTask);

    // Then
    Map<String, UUID> expectedMetaData = Map.of("scheduledTaskId", scheduledTask.getId());
    verify(fulfilmentService, times(1))
        .processPrintFulfilment(
            caze,
            scheduledTask.getPackCode(),
            scheduledTask.getId(),
            "SRM_SCHEDULED_TASK",
            expectedMetaData,
            scheduledTask.getId());
  }
}
