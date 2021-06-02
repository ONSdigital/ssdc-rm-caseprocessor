package uk.gov.ons.ssdc.caseprocessor.schedule;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContact;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.WaveOfContactRepository;

public class WaveOfContactProcessorTest {
  private final WaveOfContactRepository waveOfContactRepository =
      mock(WaveOfContactRepository.class);
  private final CaseClassifier caseClassifier = mock(CaseClassifier.class);

  @Test
  public void testExecuteClassifiers() {
    // Given
    WaveOfContact waveOfContact = setUpWaveOfContact(WaveOfContactType.PRINT);

    // when
    WaveOfContactProcessor underTest =
        new WaveOfContactProcessor(caseClassifier, waveOfContactRepository);
    underTest.createScheduledWaveOfContact(waveOfContact);

    // then
    ArgumentCaptor<WaveOfContact> waveOfContactCaptor =
        ArgumentCaptor.forClass(WaveOfContact.class);
    verify(waveOfContactRepository, times(1)).save(waveOfContactCaptor.capture());
    WaveOfContact actualWaveOfContact = waveOfContactCaptor.getAllValues().get(0);
    waveOfContact.setHasTriggered(true);
    Assertions.assertThat(actualWaveOfContact).isEqualTo(waveOfContact);

    verify(caseClassifier).enqueueCasesForWaveOfContact(eq(waveOfContact));
  }

  private WaveOfContact setUpWaveOfContact(WaveOfContactType waveOfContactType) {
    WaveOfContact waveOfContact = new WaveOfContact();
    UUID waveOfContactId = UUID.randomUUID();
    waveOfContact.setId(waveOfContactId);
    waveOfContact.setTriggerDateTime(OffsetDateTime.now());
    waveOfContact.setHasTriggered(false);

    waveOfContact.setType(waveOfContactType);

    CollectionExercise collectionExercise = new CollectionExercise();
    collectionExercise.setId(UUID.randomUUID());

    waveOfContact.setCollectionExercise(collectionExercise);

    return waveOfContact;
  }
}
