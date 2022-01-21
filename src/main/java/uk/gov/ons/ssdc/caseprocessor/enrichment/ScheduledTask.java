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