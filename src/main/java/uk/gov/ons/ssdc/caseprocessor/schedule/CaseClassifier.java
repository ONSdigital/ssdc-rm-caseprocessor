package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;

@Component
public class CaseClassifier {
  private final JdbcTemplate jdbcTemplate;

  public CaseClassifier(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void enqueueCasesForWaveOfContact(WaveOfContact waveOfContact) {
    UUID batchId = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO casev3.case_to_process (batch_id, batch_quantity, wave_of_contact_id, "
            + "caze_id) SELECT ?, COUNT(*) OVER (), ?, id FROM "
            + "casev3.cases "
            + buildWhereClause(
                waveOfContact.getCollectionExercise().getId(),
                waveOfContact.getClassifiers(),
                waveOfContact.getType()),
        batchId,
        waveOfContact.getId());
  }

  private String buildWhereClause(
      UUID collectionExerciseId, String classifiersClause, WaveOfContactType type) {
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
