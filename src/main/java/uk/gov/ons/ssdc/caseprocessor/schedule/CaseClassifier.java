package uk.gov.ons.ssdc.caseprocessor.schedule;

import static uk.gov.ons.ssdc.caseprocessor.rasrm.constants.RasRmConstants.BUSINESS_SAMPLE_DEFINITION_URL_SUFFIX;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.ons.ssdc.caseprocessor.rasrm.service.RasRmSampleSummaryCollexLinkService;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;

@Component
public class CaseClassifier {
  private final JdbcTemplate jdbcTemplate;
  private final RasRmSampleSummaryCollexLinkService rasRmSampleSummaryCollexLinkService;

  public CaseClassifier(
      JdbcTemplate jdbcTemplate,
      RasRmSampleSummaryCollexLinkService rasRmSampleSummaryCollexLinkService) {
    this.jdbcTemplate = jdbcTemplate;
    this.rasRmSampleSummaryCollexLinkService = rasRmSampleSummaryCollexLinkService;
  }

  public void enqueueCasesForActionRule(ActionRule actionRule) {
    if (actionRule
        .getCollectionExercise()
        .getSurvey()
        .getSampleDefinitionUrl()
        .endsWith(BUSINESS_SAMPLE_DEFINITION_URL_SUFFIX)) {
      // This only needs to be done once, for efficiency, but it's pretty horrible having to hack
      // it in right here. In the ideal world, there would be a more elegant place to put this
      // in the code... but its horribleness is largely due to how difficult RAS-RM APIs are
      rasRmSampleSummaryCollexLinkService.linkSampleSummaryToCollex(
          actionRule.getCollectionExercise());
    }

    UUID batchId = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO casev3.case_to_process (batch_id, batch_quantity, action_rule_id, "
            + "caze_id) SELECT ?, COUNT(*) OVER (), ?, id FROM "
            + "casev3.cases "
            + buildWhereClause(
                actionRule.getCollectionExercise().getId(), actionRule.getClassifiers()),
        batchId,
        actionRule.getId());
  }

  private String buildWhereClause(UUID collectionExerciseId, String classifiersClause) {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append(
        String.format("WHERE collection_exercise_id='%s'", collectionExerciseId.toString()));

    if (StringUtils.hasText(classifiersClause)) {
      whereClause.append(" AND ").append(classifiersClause);
    }

    return whereClause.toString();
  }
}
