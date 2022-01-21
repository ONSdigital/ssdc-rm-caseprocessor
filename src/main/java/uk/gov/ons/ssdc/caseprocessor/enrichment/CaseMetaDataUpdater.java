package uk.gov.ons.ssdc.caseprocessor.enrichment;

import uk.gov.ons.ssdc.common.model.entity.Case;

public interface CaseMetaDataUpdater {
    boolean updateCaseScheduling(Case caze);
}
