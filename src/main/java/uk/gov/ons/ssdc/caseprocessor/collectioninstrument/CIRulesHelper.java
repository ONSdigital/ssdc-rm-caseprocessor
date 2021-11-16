package uk.gov.ons.ssdc.caseprocessor.collectioninstrument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class CIRulesHelper {
  private static final ExpressionParser expressionParser = new SpelExpressionParser();

  public static FullyPreparedRule[] prepareAndSortRules(
      CollectionInstrumentSelectionRule[] unpreparedRules) {
    List<FullyPreparedRule> preparedRules = new ArrayList<>(unpreparedRules.length);

    for (CollectionInstrumentSelectionRule unpreparedRule : unpreparedRules) {
      Expression spelExpression = null;

      if (unpreparedRule.getSpelExpression() != null) {
        spelExpression = expressionParser.parseExpression(unpreparedRule.getSpelExpression());
      }

      preparedRules.add(
          new FullyPreparedRule(
              spelExpression,
              unpreparedRule.getPriority(),
              unpreparedRule.getCollectionInstrumentUrl()));
    }

    preparedRules.sort(Comparator.comparingInt(FullyPreparedRule::getPriority).reversed());

    return preparedRules.toArray(new FullyPreparedRule[preparedRules.size()]);
  }
}
