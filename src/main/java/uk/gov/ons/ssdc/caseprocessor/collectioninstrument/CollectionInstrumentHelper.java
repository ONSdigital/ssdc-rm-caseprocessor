package uk.gov.ons.ssdc.caseprocessor.collectioninstrument;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.common.model.entity.Case;

@Component
public class CollectionInstrumentHelper {
  private static final Logger log = LoggerFactory.getLogger(CollectionInstrumentHelper.class);

  private final RulesCache rulesCache;

  public CollectionInstrumentHelper(RulesCache rulesCache) {
    this.rulesCache = rulesCache;
  }

  public String getCollectionInstrumentUrl(Case caze, Object uacMetadata) {
    EvaluationBundle bundle = new EvaluationBundle(caze, uacMetadata);
    EvaluationContext context = new StandardEvaluationContext(bundle);

    CachedRule[] rules = rulesCache.getRules(caze.getCollectionExercise().getId());

    String selectedCollectionInstrumentUrl = null;
    for (CachedRule cachedRule : rules) {
      Boolean expressionResult = Boolean.TRUE;

      // No expression means "match anything"... used for 'default' rule
      if (cachedRule.getSpelExpression() != null) {
        try {
          expressionResult = cachedRule.getSpelExpression().getValue(context, Boolean.class);
        } catch (Exception spelExpressionEvaluationException) {
          log.with("case_id", caze.getId())
              .with("uac_metadata", uacMetadata)
              .with("expression", cachedRule.getSpelExpression().getExpressionString())
              .error(
                  "Collection instrument selection rule causing error",
                  spelExpressionEvaluationException);

          throw new RuntimeException(
              "Collection instrument selection rule causing error",
              spelExpressionEvaluationException);
        }
      }

      if (expressionResult) {
        selectedCollectionInstrumentUrl = cachedRule.getCollectionInstrumentUrl();
        break;
      }
    }

    return selectedCollectionInstrumentUrl;
  }
}
