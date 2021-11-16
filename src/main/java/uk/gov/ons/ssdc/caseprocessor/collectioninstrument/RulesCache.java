package uk.gov.ons.ssdc.caseprocessor.collectioninstrument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.common.model.entity.CollectionExercise;

@Component
public class RulesCache {
  private static final ExpressionParser expressionParser = new SpelExpressionParser();

  private final CollectionExerciseRepository collectionExerciseRepository;

  public RulesCache(
      CollectionExerciseRepository collectionExerciseRepository) {
    this.collectionExerciseRepository = collectionExerciseRepository;
  }

  @Cacheable("collectionInstrumentRules")
  public CachedRule[] getRules(UUID collectionExerciseId) {
    CollectionExercise collectionExercise = collectionExerciseRepository.findById(
        collectionExerciseId).orElseThrow(() -> new RuntimeException("Collex not found"));

    // TODO: get the rules off the collex instead of hard-coding them here
    CollectionInstrumentSelectionRule[] collectionInstrumentSelectionRules =
        new CollectionInstrumentSelectionRule[] {
            new CollectionInstrumentSelectionRule(
                1000,
                "caze.sample['POSTCODE'] == 'peter' and uacMetadata != null and uacMetadata['wave'] == 1",
                "http://brian/andrew"),
            new CollectionInstrumentSelectionRule(
                500, "caze.sample['POSTCODE'] == 'john'", "http://norman/george"),
            new CollectionInstrumentSelectionRule(0, null, "http://thomas/ermintrude")
        };

    return prepareAndSortRules(collectionInstrumentSelectionRules);
  }

  private CachedRule[] prepareAndSortRules(
      CollectionInstrumentSelectionRule[] unpreparedRules) {
    List<CachedRule> preparedRules = new ArrayList<>(unpreparedRules.length);

    for (CollectionInstrumentSelectionRule unpreparedRule : unpreparedRules) {
      Expression spelExpression = null;

      if (unpreparedRule.getSpelExpression() != null) {
        spelExpression = expressionParser.parseExpression(unpreparedRule.getSpelExpression());
      }

      preparedRules.add(
          new CachedRule(
              spelExpression,
              unpreparedRule.getPriority(),
              unpreparedRule.getCollectionInstrumentUrl()));
    }

    preparedRules.sort(Comparator.comparingInt(CachedRule::getPriority).reversed());

    return preparedRules.toArray(new CachedRule[preparedRules.size()]);
  }

}
