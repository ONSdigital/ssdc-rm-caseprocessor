package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;

public class CaseClassifierTest {

  @Test
  public void testEnqueueCasesForActionRulePrinter() {
    // Given
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    CaseClassifier underTest = new CaseClassifier(jdbcTemplate);
    String classifiers = "foo IN ('bar')";
    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());
    WaveOfContact waveOfContact = new WaveOfContact();
    waveOfContact.setId(UUID.randomUUID());
    waveOfContact.setCollectionExercise(collectionExercise);
    waveOfContact.setClassifiers(classifiers);
    waveOfContact.setType(WaveOfContactType.PRINT);

    // When
    underTest.enqueueCasesForWaveOfContact(waveOfContact);

    // Then
    StringBuilder expectedSql = new StringBuilder();
    expectedSql.append("INSERT INTO casev3.case_to_process (batch_id, batch_quantity,");
    expectedSql.append(" wave_of_contact_id, caze_id)");
    expectedSql.append(" SELECT ?, COUNT(*) OVER (), ?, id");
    expectedSql.append(" FROM casev3.cases WHERE collection_exercise_id=");
    expectedSql.append("'" + collectionExercise.getId().toString() + "'");
    expectedSql.append(" AND receipt_received='f'");
    expectedSql.append(" AND address_invalid='f'");
    expectedSql.append(" AND refusal_received IS NULL");
    expectedSql.append(" AND foo IN ('bar')");
    verify(jdbcTemplate)
        .update(eq(expectedSql.toString()), any(UUID.class), eq(waveOfContact.getId()));
  }
}
