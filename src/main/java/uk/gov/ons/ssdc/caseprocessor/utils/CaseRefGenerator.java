package uk.gov.ons.ssdc.caseprocessor.utils;

import uk.gov.ons.ssdc.caseprocessor.utils.pseudorandom.PseudorandomNumberGenerator;

public class CaseRefGenerator {
  //  This gives a caseRef 9 long, plus 1 checkbit at the end = 10
  private static final int LOWEST_POSSIBLE_CASE_REF = 100000000;
  private static final int HIGHEST_POSSIBLE_CASE_REF = 999999999;

  private static final PseudorandomNumberGenerator PSEUDORANDOM_NUMBER_GENERATOR =
      new PseudorandomNumberGenerator(HIGHEST_POSSIBLE_CASE_REF - LOWEST_POSSIBLE_CASE_REF);

  public static long getCaseRef(int sequenceNumber, byte[] caserefgeneratorkey) {
    // DO NOT replace this with a random number generator - we must have zero collisions/duplicates
    int pseudorandomNumber =
        PSEUDORANDOM_NUMBER_GENERATOR.getPseudorandom(sequenceNumber, caserefgeneratorkey);
    int caserefWithoutCheckDigit = pseudorandomNumber + LOWEST_POSSIBLE_CASE_REF;
    return LuhnHelper.addCheckDigit(caserefWithoutCheckDigit);
  }
}
