package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ssdc.caseprocessor.testutils.ScheduleTaskHelper.createOneTaskSimpleScheduleTemplate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.DateOffSet;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.ScheduleTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;
import uk.gov.ons.ssdc.common.model.entity.Survey;

@ExtendWith(MockitoExtension.class)
public class ScheduledTaskServiceTest {
  @Mock ScheduledTaskRepository scheduledTaskRepository;

  @InjectMocks ScheduledTaskService underTest;

  @Test
  public void testSimpleScheduleTemplateTurnedIntoScheduledTaskGroups() {
    ScheduleTemplate scheduleTemplate = createOneTaskSimpleScheduleTemplate(ChronoUnit.DAYS, 1);

    Case caze = getSimpleCaseWithinSurvey(scheduleTemplate);
    List<CaseScheduledTaskGroup> scheduledTaskGroups =
        underTest.addScheduleToDBAndReturnScheduledTaskGroups(caze);

    assertThat(scheduledTaskGroups.size()).isEqualTo(1);
    CaseScheduledTaskGroup scheduledTaskGroup = scheduledTaskGroups.get(0);
    assertThat(scheduledTaskGroup.getName()).isEqualTo("Task Group 1");
    assertThat(scheduledTaskGroup.getScheduledTasks().size()).isEqualTo(1);

    DateOffSet scheduledTaskGroupDateOffSet = scheduledTaskGroup.getDateOffsetFromTaskGroupStart();
    assertThat(scheduledTaskGroupDateOffSet.getDateUnit()).isEqualTo(ChronoUnit.DAYS);
    assertThat(scheduledTaskGroupDateOffSet.getOffset()).isEqualTo(0);

    CaseScheduledTask scheduledTask = scheduledTaskGroup.getScheduledTasks().get(0);
    assertThat(scheduledTask.getName()).isEqualTo("Task 1");
    assertThat(scheduledTask.getScheduledTaskType())
        .isEqualTo(ScheduledTaskType.ACTION_WITH_PACKCODE);

    OffsetDateTime scheduledTaskRRunDateTime =
        OffsetDateTime.parse(
            scheduledTask.getScheduledDateToRun(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    OffsetDateTime expectedTaskDateTime = OffsetDateTime.now().plusDays(1);

    assertThat(scheduledTaskRRunDateTime)
        .isCloseTo(expectedTaskDateTime, within(5, ChronoUnit.SECONDS));

    ArgumentCaptor<ScheduledTask> scheduledTaskArgumentCaptor =
        ArgumentCaptor.forClass(ScheduledTask.class);

    verify(scheduledTaskRepository).saveAndFlush(scheduledTaskArgumentCaptor.capture());
    ScheduledTask actualScheduledTask = scheduledTaskArgumentCaptor.getValue();
    assertThat(actualScheduledTask.getId()).isEqualTo(scheduledTask.getId());
    assertThat(actualScheduledTask.getScheduledTaskType())
        .isEqualTo(ScheduledTaskType.ACTION_WITH_PACKCODE);
    assertThat(actualScheduledTask.getPackCode()).isEqualTo("Test-Pack-Code");
    assertThat(actualScheduledTask.getName()).isEqualTo("Task 1");
    assertThat(actualScheduledTask.getRmToActionDate())
        .isCloseTo(expectedTaskDateTime, within(5, ChronoUnit.SECONDS));
  }

  private Case getSimpleCaseWithinSurvey(ScheduleTemplate scheduleTemplate) {
    Survey survey = new Survey();
    survey.setScheduleTemplate(scheduleTemplate);

    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setSurvey(survey);

    Case caze = new Case();
    caze.setId(UUID.randomUUID());
    caze.setCollectionExercise(collectionExercise);

    return caze;
  }
}
