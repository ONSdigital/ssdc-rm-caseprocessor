package uk.gov.ons.ssdc.caseprocessor.enrichment;

import uk.gov.ons.ssdc.common.model.entity.Case;

public class IndividualCaseScheduling implements CaseMetaDataUpdater {

    // This could also be called by uac/qid updates.
    // Or horrifically some Survey wide event to move all cases dates, adjust scheduling etc?

    @Override
    public boolean updateCaseScheduling(Case caze) {
        //  Here we update scheduling for this case if required

        /*
            With this class would go whatever full horror of complex case scheduling is required
            At the end of it we may end up with
         */



        return false;
    }
}
