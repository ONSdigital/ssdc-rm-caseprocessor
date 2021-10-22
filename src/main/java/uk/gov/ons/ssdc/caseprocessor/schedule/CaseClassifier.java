package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.ons.ssdc.caseprocessor.client.RasRmPartyServiceClient;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;

@Component
public class CaseClassifier {
  private static final Set<String> MANDATORY_COLLEX_METADATA =
      Set.of("rasRmSampleSummaryId", "rasRmCollectionExerciseId");

  private final JdbcTemplate jdbcTemplate;
  private final RasRmPartyServiceClient rasRmPartyServiceClient;

  public CaseClassifier(
      JdbcTemplate jdbcTemplate, RasRmPartyServiceClient rasRmPartyServiceClient) {
    this.jdbcTemplate = jdbcTemplate;
    this.rasRmPartyServiceClient = rasRmPartyServiceClient;
  }

  public void enqueueCasesForActionRule(ActionRule actionRule) {
    if (actionRule
        .getCollectionExercise()
        .getSurvey()
        .getSampleDefinitionUrl()
        .endsWith("business.json")) {
      Object metadataObject = actionRule.getCollectionExercise().getMetadata();

      if (metadataObject == null) {
        throw new RuntimeException(
            "Unexpected null metadata. Metadata is required for RAS-RM business.");
      }

      if (!(metadataObject instanceof Map)) {
        throw new RuntimeException(
            "Unexpected metadata type. Wanted Map but got "
                + metadataObject.getClass().getSimpleName());
      }

      Map metadata = (Map) metadataObject;

      if (!metadata.keySet().containsAll(MANDATORY_COLLEX_METADATA)) {
        throw new RuntimeException("Metadata does not contain mandatory values");
      }

      UUID rasRmSampleSummaryId = UUID.fromString((String) metadata.get("rasRmSampleSummaryId"));
      UUID rasRmCollectionExerciseId =
          UUID.fromString((String) metadata.get("rasRmCollectionExerciseId"));

      // This only needs to be done once, for efficiency, but it's pretty horrible having to hack
      // it in right here. In the ideal world, there would be a more elegant place to put this
      // in the code... but its horribleness is largely due to how difficult RAS-RM APIs are
      rasRmPartyServiceClient.linkSampleSummaryToCollex(
          rasRmSampleSummaryId, rasRmCollectionExerciseId);
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
