package uk.gov.ons.ssdc.caseprocessor.enrichment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduledTask {
//    probably UTC actually
    private LocalDateTime runAtOrAfter;
//    actually an enum.  Run what?  PCR, Blood etc
    private String packCode;  //Would this just be a packCode? captures template, target etc?
}

/*
    Would this be just for cases?  I think so..
    Would GIN indexes make this long term viable?
    Imagine several million cases of various surveys, and selecting through each one every time?
*/

/*
    Also these scheduled tasks would be exposed to the frontend/api?
    Would we allow the frontend/api/msg to update the scheduledTasks?  Yes - but how to stop automatic update?
    This gets tricky
    A user could suspend/delete/mark a scheduled task for a holiday
    But if there was plausibly a survey wide scheduling update (say moving to every 2 months) - then all the schedule Task
    lists would be regenerated?
*/

/*
    With MI we could outsource entirely this scheduling to the business?
    You tell us what to run, when and for what?  Pass us a scheduled task  list per case or collectionExercise?
    We just process these
 */