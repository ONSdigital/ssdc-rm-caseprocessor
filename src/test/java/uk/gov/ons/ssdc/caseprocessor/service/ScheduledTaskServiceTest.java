package uk.gov.ons.ssdc.caseprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.data.TemporalUnitOffset;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DateOffSet;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplateTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplateTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ScheduledTaskRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;
import uk.gov.ons.ssdc.common.model.entity.Survey;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ScheduledTaskServiceTest {
    @Mock
    ScheduledTaskRepository scheduledTaskRepository;

    @InjectMocks
    ScheduledTaskService underTest;

    @Test
    public void testSimpleScheduleTemplateTurnedIntoScheduledTaskGroups() throws JsonProcessingException {
        ScheduleTemplate scheduleTemplate = createOneTaskSimpleScheduleTemplate();

        Case caze = getSimpleCaseWithinSurvey(scheduleTemplate);
        List<CaseScheduledTaskGroup> scheduledTaskGroups = underTest.addScheduleToDBAndReturnScheduledTaskGroups(caze);

        assertThat(scheduledTaskGroups.size()).isEqualTo(1);
        CaseScheduledTaskGroup scheduledTaskGroup = scheduledTaskGroups.get(0);
        assertThat(scheduledTaskGroup.getName()).isEqualTo("Task Group 1");
        assertThat(scheduledTaskGroup.getScheduledTasks().size()).isEqualTo(1);

        DateOffSet scheduledTaskGroupDateOffSet = scheduledTaskGroup.getDateOffsetFromTaskGroupStart();
        assertThat(scheduledTaskGroupDateOffSet.getDateUnit()).isEqualTo(ChronoUnit.DAYS);
        assertThat(scheduledTaskGroupDateOffSet.getOffset()).isEqualTo(0);

        CaseScheduledTask scheduledTask = scheduledTaskGroup.getScheduledTasks().get(0);
        assertThat(scheduledTask.getName()).isEqualTo("Task 1");
        assertThat(scheduledTask.getScheduledTaskType()).isEqualTo(ScheduledTaskType.ACTION_WITH_PACKCODE);

        OffsetDateTime scheduledTaskRRunDateTime
                = OffsetDateTime.parse(scheduledTask.getScheduledDateToRun(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        OffsetDateTime expectedTaskDateTime = OffsetDateTime.now().plusDays(1);

        assertThat(scheduledTaskRRunDateTime).isCloseTo(expectedTaskDateTime, within(5, ChronoUnit.SECONDS));
    }

    private Case getSimpleCaseWithinSurvey(ScheduleTemplate scheduleTemplate) {
        Survey survey = new Survey();
        survey.setScheduleTemplate(convertObjectToJson(scheduleTemplate));

        CollectionExercise collectionExercise = new CollectionExercise();
        collectionExercise.setSurvey(survey);

        Case caze = new Case();
        caze.setId(UUID.randomUUID());
        caze.setCollectionExercise(collectionExercise);

        return caze;
    }

    private ScheduleTemplate createOneTaskSimpleScheduleTemplate() {
        ScheduleTemplateTask scheduleTemplateTask = new ScheduleTemplateTask();
        scheduleTemplateTask.setName("Task 1");
        scheduleTemplateTask.setScheduledTaskType(ScheduledTaskType.ACTION_WITH_PACKCODE);
        scheduleTemplateTask.setDateOffSetFromStart(new DateOffSet(ChronoUnit.DAYS, 1));

        ScheduleTemplateTaskGroup scheduleTemplateTaskGroup = new ScheduleTemplateTaskGroup();
        scheduleTemplateTaskGroup.setName("Task Group 1");
        scheduleTemplateTaskGroup.setScheduleTemplateTasks(List.of(scheduleTemplateTask));
        scheduleTemplateTaskGroup.setDateOffsetFromTaskGroupStart((new DateOffSet(ChronoUnit.DAYS, 0)));

        ScheduleTemplateTaskGroup[] scheduleTemplateTaskGroups = new ScheduleTemplateTaskGroup[]{scheduleTemplateTaskGroup};

        ScheduleTemplate scheduleTemplate = new ScheduleTemplate();
        scheduleTemplate.setName("Simple Schedule Template");
        scheduleTemplate.setScheduleTemplateTaskGroups(scheduleTemplateTaskGroups);

        return scheduleTemplate;
    }

    public String convertObjectToJson(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed converting Object To Json", e);
        }
    }
}