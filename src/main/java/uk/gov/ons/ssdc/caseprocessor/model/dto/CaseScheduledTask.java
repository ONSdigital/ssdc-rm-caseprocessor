package uk.gov.ons.ssdc.caseprocessor.model.dto;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import src.main.java.uk.gov.ons.ssdc.common.model.entity.DateOffSet;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;

@Data
public class CaseScheduledTask {
  private UUID id;
  private String name;
  private ScheduledTaskType scheduledTaskType;
  private String packCode;
  private DateOffSet dateOffSetFromStart;

  private String scheduledDateToRun;

  // These are for auditing purposes, if a new UAC was requested we would want to record that too.
  private List<UUID> uacsIds;
  private List<UUID> eventIds;
}
