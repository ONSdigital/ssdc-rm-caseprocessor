package uk.gov.ons.ssdc.caseprocessor.service;

public class ScheduledTaskService {

  //    public List<ResponsePeriod> buildResponsePeriodAndScheduledTasks(Case caze)
  //            throws JsonProcessingException {
  //        ObjectMapper objectMapper = new ObjectMapper();
  //
  //        ScheduleTemplate scheduleTemplate =
  //                objectMapper.readValue(
  //                        (String) caze.getCollectionExercise().getSurvey().getScheduleTemplate(),
  //                        ScheduleTemplate.class);
  //
  //        OffsetDateTime startOfResponsePeriod =
  // OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
  //
  //        int iterationCount = 1;
  //        List<ResponsePeriod> responsePeriodList = new ArrayList<>();
  //
  //        for (DateOffSet dateOffSet : scheduleTemplate.getTaskSpacing()) {
  //            ResponsePeriod responsePeriod = new ResponsePeriod();
  //            responsePeriod.setId(UUID.randomUUID());
  //            responsePeriod.setName(scheduleTemplate.getName() + " " + iterationCount);
  //            responsePeriod.setCaze(caze);
  //            responsePeriodRepository.saveAndFlush(responsePeriod);
  //
  //            List<ScheduledTask> scheduledTasks =
  //                    buildScheduledTaskList(
  //                            scheduleTemplate.getTasks(), startOfResponsePeriod, responsePeriod);
  //            responsePeriod.setScheduledTasks(scheduledTasks);
  //
  //            responsePeriodRepository.saveAndFlush(responsePeriod);
  //            responsePeriodList.add(responsePeriod);
  //
  //            startOfResponsePeriod = getOffDate(startOfResponsePeriod, dateOffSet);
  //            iterationCount++;
  //        }
  //
  //        return responsePeriodList;
  //    }
  //
  //    private List<ScheduledTaskDTO> buildScheduledTaskList(
  //            Task[] tasks, OffsetDateTime startDate, ResponsePeriod responsePeriod) {
  //        List<ScheduledTaskDTO> scheduledTasks = new ArrayList<>();
  //
  //        for (Task task : tasks) {
  //            ScheduledTask scheduledTask = new ScheduledTask();
  //            scheduledTask.setId(UUID.randomUUID());
  //            scheduledTask.setTaskName(responsePeriod.getName() + " " + task.getName());
  //            scheduledTask.setReceiptRequiredForCompletion(task.isReceiptRequired());
  //
  //            OffsetDateTime dateToStart = getOffDate(startDate, task.getDateOffSet());
  //            scheduledTask.setRmToActionDate(dateToStart);
  //
  //            /* This could be quite different? */
  //            /* A different Type might expect SPEL?,  Or just use SPEL */
  //            /* So there could be an extra field: SPEL, or just a SPEL field - which might be
  // more elegant? */
  //            Map<String, String> details =
  //                    Map.of("type", task.getScheduledTaskType().toString(), "packCode",
  // task.getPackCode());
  //
  //            scheduledTask.setScheduledTaskDetails(details);
  //            scheduledTask.setActionState(ScheduledTaskState.NOT_STARTED);
  //            scheduledTask.setResponsePeriod(responsePeriod);
  //
  //            scheduledTask = scheduledTaskRepository.saveAndFlush(scheduledTask);
  //
  //            scheduledTasks.add(scheduledTask);
  //        }
  //
  //        return scheduledTasks;
  //    }
  //
  //    private OffsetDateTime getOffDate(OffsetDateTime startDate, DateOffSet dateOffSet) {
  //
  //        switch (dateOffSet.getDateUnit()) {
  //            case DAY:
  //                return startDate.plusDays(dateOffSet.getMultiplier());
  //            case WEEK:
  //                return startDate.plusWeeks(dateOffSet.getMultiplier());
  //
  //            case MONTH:
  //                return startDate.plusMonths(dateOffSet.getMultiplier());
  //        }
  //
  //        throw new RuntimeException("Can't get to getRMActionDate exception, it's enums");
  //    }

}
