package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.ResponsePeriod;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTask;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskState;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ScheduledTaskBuilder {

    public ResponsePeriod [] buildResponsePeriodAndScheduledTasks(Case caze) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        ScheduleTemplate scheduleTemplate = objectMapper.readValue(
                (String) caze.getCollectionExercise().getSurvey().getScheduleTemplate(), ScheduleTemplate.class);

        // build response periods with same OffSet function
        // for each one add scheduledTasks as we go.



//        List<ResponsePeriod> responsePeriodList = new ArrayList<>();

    }

    private List<ScheduledTask> buildScheduledTaskList(Task [] tasks, OffsetDateTime startDate, String periodName) {
        List<ScheduledTask> scheduledTasks = new ArrayList<>();

        for(Task task : tasks) {
            ScheduledTask scheduledTask = new ScheduledTask();
            scheduledTask.setId(UUID.randomUUID());
            scheduledTask.setTaskName(periodName + " " + task.getName());
            scheduledTask.setReceiptRequiredForCompletion(task.isReceiptRequired());

            OffsetDateTime dateToStart = getRMActionDate(startDate, task.getDateOffSet());
            scheduledTask.setRmToActionDate(dateToStart);

            Map<String, String> details =
                    Map.of(
                            "type",
                            task.getScheduledTaskType().toString(),
                            "packCode",
                            task.getPackCode());
            scheduledTask.setScheduledTaskDetails(details);
            scheduledTask.setActionState(ScheduledTaskState.NOT_STARTED);

            scheduledTasks.add(scheduledTask);
        }

        return scheduledTasks;
    }

    private OffsetDateTime getRMActionDate(OffsetDateTime startDate, DateOffSet dateOffSet) {

        switch(dateOffSet.getDateUnit()) {
            case DAY:
                return startDate.plusDays(dateOffSet.getMultiplier());

            case WEEK:
                return startDate.plusWeeks(dateOffSet.getMultiplier());

            case MONTH:
                return startDate.plusMonths(dateOffSet.getMultiplier());
        }

        throw new RuntimeException("Can't get to getRMActionDate exception, it's enums");
    }
}
