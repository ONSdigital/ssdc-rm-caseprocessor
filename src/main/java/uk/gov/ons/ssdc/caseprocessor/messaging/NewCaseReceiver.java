package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.rasrm.constants.RasRmConstants.BUSINESS_SAMPLE_DEFINITION_URL_SUFFIX;
import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseScheduledTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.NewCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplateTask;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ScheduleTemplateTaskGroup;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.rasrm.service.RasRmCaseNotificationEnrichmentService;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.ScheduledTaskService;
import uk.gov.ons.ssdc.caseprocessor.utils.CaseRefGenerator;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.Survey;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;

@MessageEndpoint
public class NewCaseReceiver {
  private final CaseRepository caseRepository;
  private final CaseService caseService;
  private final CollectionExerciseRepository collectionExerciseRepository;
  private final EventLogger eventLogger;
  private final RasRmCaseNotificationEnrichmentService rasRmNewBusinessCaseEnricher;
  private final ScheduledTaskService scheduledTaskService;

  @Value("${caserefgeneratorkey}")
  private byte[] caserefgeneratorkey;

  public NewCaseReceiver(
      CaseRepository caseRepository,
      CaseService caseService,
      CollectionExerciseRepository collectionExerciseRepository,
      EventLogger eventLogger,
      RasRmCaseNotificationEnrichmentService rasRmNewBusinessCaseEnricher,
      ScheduledTaskService scheduledTaskService) {
    this.caseRepository = caseRepository;
    this.caseService = caseService;
    this.collectionExerciseRepository = collectionExerciseRepository;
    this.eventLogger = eventLogger;
    this.rasRmNewBusinessCaseEnricher = rasRmNewBusinessCaseEnricher;
    this.scheduledTaskService = scheduledTaskService;
  }

  @Transactional
  @ServiceActivator(inputChannel = "newCaseInputChannel", adviceChain = "retryAdvice")
  public void receiveNewCase(Message<byte[]> message) throws JsonProcessingException, SQLException {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    NewCase newCasePayload = event.getPayload().getNewCase();

    if (caseRepository.existsById(newCasePayload.getCaseId())) {
      // Case already exists, so let's not overwrite it... swallow the message quietly
      return;
    }

    CollectionExercise collex =
        collectionExerciseRepository
            .findById(newCasePayload.getCollectionExerciseId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Collection exercise '"
                            + newCasePayload.getCollectionExerciseId()
                            + "' not found"));

    ColumnValidator[] columnValidators = collex.getSurvey().getSampleValidationRules();
    checkNewSampleWithinSampleDefinition(columnValidators, newCasePayload);
    checkNewSensitiveWithinSampleSensitiveDefinition(columnValidators, newCasePayload);

    validateNewCase(newCasePayload, columnValidators);

    Map<String, String> sample = newCasePayload.getSample();

    if (collex
        .getSurvey()
        .getSampleDefinitionUrl()
        .endsWith(BUSINESS_SAMPLE_DEFINITION_URL_SUFFIX)) {
      sample =
          rasRmNewBusinessCaseEnricher.notifyRasRmAndEnrichSample(sample, collex.getMetadata());
    }

    Case newCase = new Case();
    newCase.setId(newCasePayload.getCaseId());
    newCase.setCollectionExercise(collex);

    List<CaseScheduledTaskGroup> caseScheduledTaskGroups = getSchedule(collex.getSurvey());

    newCase.setSchedule(caseScheduledTaskGroups);

    newCase.setSample(sample);
    newCase.setSampleSensitive(newCasePayload.getSampleSensitive());

    newCase = saveNewCaseAndStampCaseRef(newCase);
    caseService.emitCaseUpdate(
        newCase, event.getHeader().getCorrelationId(), event.getHeader().getOriginatingUser());

    if (caseScheduledTaskGroups != null) {
      scheduledTaskService.createScheduledTasksFromSchedulePlan(
          caseScheduledTaskGroups, newCase.getId());
    }

    eventLogger.logCaseEvent(newCase, "New case created", EventType.NEW_CASE, event, message);
  }

  private Set<String> checkNewSensitiveWithinSampleSensitiveDefinition(
      ColumnValidator[] columnValidators, NewCase newCasePayload) {
    Set<String> sensitiveColumns =
        Arrays.stream(columnValidators)
            .filter(ColumnValidator::isSensitive)
            .map(ColumnValidator::getColumnName)
            .collect(Collectors.toSet());
    if (!sensitiveColumns.containsAll(newCasePayload.getSampleSensitive().keySet())) {
      throw new RuntimeException(
          "Attempt to send sensitive data to RM which was not part of defined sample");
    }

    return sensitiveColumns;
  }

  private void checkNewSampleWithinSampleDefinition(
      ColumnValidator[] columnValidators, NewCase newCasePayload) {
    Set<String> nonSensitiveColumns =
        Arrays.stream(columnValidators)
            .filter(columnValidator -> !columnValidator.isSensitive())
            .map(ColumnValidator::getColumnName)
            .collect(Collectors.toSet());
    if (!nonSensitiveColumns.containsAll(newCasePayload.getSample().keySet())) {
      throw new RuntimeException("Attempt to send data to RM which was not part of defined sample");
    }
  }

  private void validateNewCase(NewCase newCasePayload, ColumnValidator[] columnValidators) {
    List<String> validationErrors = new ArrayList<>();

    for (ColumnValidator columnValidator : columnValidators) {
      if (columnValidator.isSensitive()) {
        columnValidator
            .validateRow(newCasePayload.getSampleSensitive(), true)
            .ifPresent(validationErrors::add);
      } else {
        columnValidator
            .validateRow(newCasePayload.getSample(), true)
            .ifPresent(validationErrors::add);
      }
    }

    if (!validationErrors.isEmpty()) {
      throw new RuntimeException(
          "NEW_CASE event: "
              + validationErrors.stream().collect(Collectors.joining(System.lineSeparator())));
    }
  }

  private Case saveNewCaseAndStampCaseRef(Case caze) {
    Case newCase = caseRepository.saveAndFlush(caze);
    newCase.setCaseRef(
        CaseRefGenerator.getCaseRef(newCase.getSecretSequenceNumber(), caserefgeneratorkey));
    return caseRepository.saveAndFlush(newCase);
  }

  // Hove this off into a process,  and this function to orchastrate not chain
  private List<CaseScheduledTaskGroup> getSchedule(Survey survey) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    // needs something like this, will do for now.
    if (survey.getScheduleTemplate() == null
        || survey.getScheduleTemplate().toString().length() == 0) {
      return null;
    }

    ScheduleTemplate scheduleTemplate =
        objectMapper.readValue((String) survey.getScheduleTemplate(), ScheduleTemplate.class);

    return createSchedule(scheduleTemplate);
  }

  private List<CaseScheduledTaskGroup> createSchedule(ScheduleTemplate scheduleTemplate) {

    List<CaseScheduledTaskGroup> caseScheduledTaskGroups = new ArrayList<>();
    // why add 5 seconds, well because of ATs.
    // This certainly isn't perfect
    // TODO: remove test crutch in production code.
    OffsetDateTime caseTaskGroupStartDate = OffsetDateTime.now().plusSeconds(5);

    for (ScheduleTemplateTaskGroup scheduleTemplateTaskGroup :
        scheduleTemplate.getScheduleTemplateTaskGroups()) {
      CaseScheduledTaskGroup caseScheduledTaskGroup = new CaseScheduledTaskGroup();
      caseScheduledTaskGroup.setName(scheduleTemplateTaskGroup.getName());

      caseScheduledTaskGroup.setDateOffSet(scheduleTemplateTaskGroup.getDateOffSet());

      caseScheduledTaskGroups.add(caseScheduledTaskGroup);

      caseTaskGroupStartDate =
          caseTaskGroupStartDate.plusSeconds(
              caseScheduledTaskGroup.getDateOffSet().getDateUnit().getDuration().getSeconds()
                  * caseScheduledTaskGroup.getDateOffSet().getOffset());

      caseScheduledTaskGroup.setScheduledTasks(
          createScheduledTaskList(scheduleTemplateTaskGroup, caseTaskGroupStartDate));
    }

    return caseScheduledTaskGroups;
  }

  private List<CaseScheduledTask> createScheduledTaskList(
      ScheduleTemplateTaskGroup scheduleTemplateTaskGroup, OffsetDateTime startDate) {
    List<CaseScheduledTask> caseScheduledTaskList = new ArrayList<>();

    for (ScheduleTemplateTask scheduleTemplateTask :
        scheduleTemplateTaskGroup.getScheduleTemplateTasks()) {
      CaseScheduledTask caseScheduledTask = new CaseScheduledTask();
      caseScheduledTask.setId(UUID.randomUUID());
      caseScheduledTask.setName(scheduleTemplateTask.getName());
      caseScheduledTask.setScheduledTaskType(scheduleTemplateTask.getScheduledTaskType());
      caseScheduledTask.setPackCode(scheduleTemplateTask.getPackCode());

      OffsetDateTime newTime =
          startDate.plusSeconds(
              scheduleTemplateTask.getDateOffSet().getDateUnit().getDuration().getSeconds()
                  * scheduleTemplateTask.getDateOffSet().getOffset());

      caseScheduledTask.setRmScheduledDateTime(newTime);
      caseScheduledTask.setScheduledDateAsString(newTime.toString());

      caseScheduledTask.setDateOffSet(scheduleTemplateTask.getDateOffSet());
      caseScheduledTask.setEventIds(new ArrayList<>());
      caseScheduledTask.setUacsIds(new ArrayList<>());

      caseScheduledTaskList.add(caseScheduledTask);
    }

    return caseScheduledTaskList;
  }
}
