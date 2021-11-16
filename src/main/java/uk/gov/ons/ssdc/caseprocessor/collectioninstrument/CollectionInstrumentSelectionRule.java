package uk.gov.ons.ssdc.caseprocessor.collectioninstrument;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CollectionInstrumentSelectionRule {
  private int priority;
  private String spelExpression;
  private String collectionInstrumentUrl;
}
