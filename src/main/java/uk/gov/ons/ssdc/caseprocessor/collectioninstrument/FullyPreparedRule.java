package uk.gov.ons.ssdc.caseprocessor.collectioninstrument;

import lombok.Value;
import org.springframework.expression.Expression;

@Value
public class FullyPreparedRule {
  Expression spelExpression;
  int priority;
  String collectionInstrumentUrl;
}
