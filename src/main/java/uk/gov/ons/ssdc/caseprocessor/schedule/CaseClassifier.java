package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.ons.ssdc.caseprocessor.client.RasRmSampleServiceClient;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleType;

@Component
public class CaseClassifier {

  private final JdbcTemplate jdbcTemplate;
  private final RasRmSampleServiceClient rasRmSampleServiceClient;

  public CaseClassifier(
      JdbcTemplate jdbcTemplate, RasRmSampleServiceClient rasRmSampleServiceClient) {
    this.jdbcTemplate = jdbcTemplate;
    this.rasRmSampleServiceClient = rasRmSampleServiceClient;
  }

  public void enqueueCasesForActionRule(ActionRule actionRule) {
    UUID batchId;

    if (actionRule.getType() == ActionRuleType.SHARE_WITH_RASRM) {
      int caseCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM casev3.cases "
                  + buildWhereClause(
                      actionRule.getCollectionExercise().getId(), actionRule.getClassifiers()),
              Integer.class);

      // TODO: has been hard-coded to 1 collection instrument, for now
      batchId = rasRmSampleServiceClient.createSampleSummary(caseCount, 1).getId();
    } else {
      batchId = UUID.randomUUID();
    }

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
