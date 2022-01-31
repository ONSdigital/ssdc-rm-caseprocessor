package uk.gov.ons.ssdc.caseprocessor.scheduled.tasks;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DateOffSet {
    private DateUnit dateUnit;
    private int multiplier;
}
