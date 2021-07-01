package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRule;
import uk.gov.ons.ssdc.caseprocessor.model.entity.ActionRuleType;

@Component
public class CaseClassifier {
  private final JdbcTemplate jdbcTemplate;

  public CaseClassifier(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void enqueueCasesForActionRule(ActionRule actionRule) {
    UUID batchId = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO casev3.case_to_process (batch_id, batch_quantity, action_rule_id, "
            + "caze_id) SELECT ?, COUNT(*) OVER (), ?, id FROM "
            + "casev3.cases "
            + buildWhereClause(
                actionRule.getCollectionExercise().getId(),
                actionRule.getClassifiers(),
                actionRule.getType()),
        batchId,
        actionRule.getId());
  }

  private String buildWhereClause(
      UUID collectionExerciseId, String classifiersClause, ActionRuleType type) {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append(
        String.format("WHERE collection_exercise_id='%s'", collectionExerciseId.toString()));
    whereClause.append(" AND receipt_received='f'");
    whereClause.append(" AND address_invalid='f'");
    whereClause.append(" AND refusal_received IS NULL");

    /*
     " AND " + classifiersclause, if there's no classifiersclause (it can't be null) it will cause an error,
      as the SQL statement will end: " AND "
      However this may not be bad behaviour, we would always want to a classifiersclause
    */
    whereClause.append(" AND ").append(classifiersClause);

    return whereClause.toString();
  }
}
