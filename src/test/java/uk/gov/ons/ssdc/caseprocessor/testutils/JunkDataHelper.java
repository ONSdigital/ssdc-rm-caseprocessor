package uk.gov.ons.ssdc.caseprocessor.testutils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.SurveyRepository;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;
import uk.gov.ons.ssdc.common.model.entity.Survey;
import uk.gov.ons.ssdc.common.validation.ColumnValidator;
import uk.gov.ons.ssdc.common.validation.MandatoryRule;
import uk.gov.ons.ssdc.common.validation.Rule;

@Component
@ActiveProfiles("test")
public class JunkDataHelper {
  private static final Random RANDOM = new Random();

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private SurveyRepository surveyRepository;

  public Case setupJunkCase() {
    Case junkCase = new Case();
    junkCase.setId(UUID.randomUUID());
    junkCase.setInvalid(false);
    junkCase.setCollectionExercise(setupJunkCollex());
    junkCase.setCaseRef(RANDOM.nextLong());
    junkCase.setSample(Map.of("foo", "bar"));
    junkCase.setSampleSensitive(Map.of("phoneNumber", "123"));
    caseRepository.save(junkCase);

    return junkCase;
  }

  public CollectionExercise setupJunkCollex() {
    Survey junkSurvey = new Survey();
    junkSurvey.setId(UUID.randomUUID());
    junkSurvey.setName("Junk survey");
    junkSurvey.setSampleValidationRules(
        new ColumnValidator[] {
          new ColumnValidator("Junk", false, new Rule[] {new MandatoryRule()}),
          new ColumnValidator("SensitiveJunk", true, new Rule[] {new MandatoryRule()})
        });
    junkSurvey.setSampleSeparator('j');
    junkSurvey.setSampleDefinitionUrl("http://junk");
    surveyRepository.saveAndFlush(junkSurvey);

    CollectionExercise junkCollectionExercise = new CollectionExercise();
    junkCollectionExercise.setId(UUID.randomUUID());
    junkCollectionExercise.setName("Junk collex");
    junkCollectionExercise.setSurvey(junkSurvey);
    collectionExerciseRepository.saveAndFlush(junkCollectionExercise);

    return junkCollectionExercise;
  }

  public void junkify(EventHeaderDTO eventHeaderDTO) {
    if (eventHeaderDTO.getChannel() == null) {
      eventHeaderDTO.setChannel("Junk");
    }

    if (eventHeaderDTO.getSource() == null) {
      eventHeaderDTO.setSource("Junk");
    }

    if (eventHeaderDTO.getCorrelationId() == null) {
      eventHeaderDTO.setCorrelationId(UUID.randomUUID());
    }

    if (eventHeaderDTO.getMessageId() == null) {
      eventHeaderDTO.setMessageId(UUID.randomUUID());
    }

    if (eventHeaderDTO.getDateTime() == null) {
      eventHeaderDTO.setDateTime(OffsetDateTime.now());
    }
  }
}
