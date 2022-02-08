package uk.gov.ons.ssdc.caseprocessor.model.dto;

import lombok.Data;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskStatus;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;

import java.util.List;
import java.util.UUID;

@Data
public class ScheduledTaskDTO {
    private UUID id;
    private String name;
    private ScheduledTaskType scheduledTaskType;
    private String packCode;
    private boolean receiptRequired;
    private DateOffSet dateOffSet;
    private String rmScheduledDateTime;
    private ScheduledTaskStatus scheduledTaskStatus;

    // These are for auditing purposes, if a new UAC was requested we would want to record that too.
    private List<UUID> uacsIds;
    private List<UUID> eventIds;
}
