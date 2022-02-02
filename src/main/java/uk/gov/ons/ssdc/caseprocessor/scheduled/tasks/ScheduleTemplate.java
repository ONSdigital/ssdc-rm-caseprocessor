package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;


import lombok.Data;

import java.time.OffsetDateTime;

/*
   First Stab very simple,  Like for Monthly Reminder, PCR, EQ, INCENTIVE.
   Then maybe something to get over the idea of run weekly for 4 weeks, then monthly.

   Though this could be as simple as specifying

   schedule:  {
               type: REPEAT
               spacing: [1W,1W,1W,1W,1M,1M,1M,1M,1M,1M,1M,1M,1M,1M,1M]
               scheduleFrom: {
                                 Creation: true
                                 StartDate: Otherwise
                             },
               tasks_every_period [
                        {
                            name: reminder,
                            type: ACTION_WITH_PACKCDDE,
                            packCode: "CIS_REMINDER"
                            receipt_required: false,
                            OffSet: 0D
                         },
                         {
                            name: PCR,
                            type: ACTION_WITH_PACKCDDE,
                            packCode: "CIS_PCR"
                            receipt_required: true,
                            OffSet: 7D
                         },
                         {
                            name: EQ,
                            type: ACTION_WITH_PACKCDDE,
                            packCode: "CIS_EQ",
                            receipt_required: true,
                            OffSet: 7D
                         },
                         {
                            name: EQ,
                            type: INCENTIVE_CHECK(PCR,EQ),
                            packCode: "CIS_EQ",
                            receipt_required: false,
                            OffSet: 8D
                         },
                         {
                            name: EQ,
                            type: !COMPLETENESS_CHECK(REMINDER,PCR,EQ),
                            packCode: "CIS_COMPLETE_FAILED",
                            receipt_required: false,
                            OffSet: 14D // should be scheduled at Setup, or adjusted at completion of Sending EQ/PCR.
                                           Too complex, for now
                         }
               }

              }
 */

@Data
public class ScheduleTemplate {
    private String name;
    private TemplateType type;
    private DateOffSet [] taskSpacing;
    private boolean scheduleFromCreate;
    private OffsetDateTime startDate;
    private Task[] tasks;
}
