package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;


/* e.g

         {
             name: reminder,
             type: ACTION_WITH_PACKCDDE,
             packCode: "CIS_REMINDER"
             receipt_required: false,
             OffSet: 0D
          },
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ssdc.common.model.entity.ScheduledTaskType;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    private String name;
    private ScheduledTaskType scheduledTaskType;
    private String packCode;
    private boolean receiptRequired;
    private DateOffSet dateOffSet;
}
