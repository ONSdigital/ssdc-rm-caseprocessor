package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.ActionRuleType;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.Survey;

public class CaseClassifierTest {

  @Test
  public void testEnqueueCasesForActionRulePrinter() {
    // Given
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    CaseClassifier underTest = new CaseClassifier(jdbcTemplate, null);
    String classifiers = "foo IN ('bar')";
    Survey survey = new Survey();
    survey.setSampleDefinitionUrl("testDefinition");
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    collectionExercise.setSurvey(survey);
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setCollectionExercise(collectionExercise);
    actionRule.setClassifiers(classifiers);
    actionRule.setType(ActionRuleType.PRINT);

    // When
    underTest.enqueueCasesForActionRule(actionRule);

    // Then
    StringBuilder expectedSql = new StringBuilder();
    expectedSql.append("INSERT INTO casev3.case_to_process (batch_id, batch_quantity,");
    expectedSql.append(" action_rule_id, caze_id)");
    expectedSql.append(" SELECT ?, COUNT(*) OVER (), ?, id");
    expectedSql.append(" FROM casev3.cases WHERE collection_exercise_id=");
    expectedSql.append("'" + collectionExercise.getId().toString() + "'");
    expectedSql.append(" AND foo IN ('bar')");
    verify(jdbcTemplate)
        .update(eq(expectedSql.toString()), any(UUID.class), eq(actionRule.getId()));
  }
}
