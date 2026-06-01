package uk.gov.ons.ssdc.caseprocessor.schedule;

import static uk.gov.ons.ssdc.common.model.entity.ActionRuleType.REMOVE_PERSONAL_DATA;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;

@Component
public class CaseClassifier {
  private final JdbcTemplate jdbcTemplate;

  public CaseClassifier(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int enqueueCasesForActionRule(ActionRule actionRule) {
    UUID batchId = UUID.randomUUID();

    if (actionRule.getType().equals(REMOVE_PERSONAL_DATA)) {
      return jdbcTemplate.update(
          "INSERT INTO casev3.case_to_process (batch_id, batch_quantity, action_rule_id, "
              + "caze_id, to_be_deleted) SELECT ?, COUNT(*) OVER (), ?, id, ? FROM "
              + "casev3.cases "
              + buildWhereClause(
                  actionRule.getCollectionExercise().getId(), actionRule.getClassifiers()),
          batchId,
          actionRule.getId(),
          true);
    }

    return jdbcTemplate.update(
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
