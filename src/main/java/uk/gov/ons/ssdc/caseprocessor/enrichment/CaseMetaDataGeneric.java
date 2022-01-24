package uk.gov.ons.ssdc.caseprocessor.enrichment;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CaseMetaDataGeneric {
 // probably an ENUM
    private String metaDataType;
   // more generic preferences
    private Preferences preferences;

//    This would be sorted by next scheduled task,  e.g. scheduledTasks[0]
//    This should be doable with the wonder of Java8+ scheduleTasks = scheduledTasks.filter().sort(date).asec().  etc?
//    We'd do a performance test to see if this was plausible, definitely prefereable to caching.
//    The hourly/daily action rules picking this up would work as is?
    private List<ScheduledTask> scheduledTasks;

    private List<ScheduledTask> scheduledTasksHistory; // might just be EventLogger to record changes here?
}
