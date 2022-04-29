package uk.gov.ons.ssdc.caseprocessor.testutils;

public class ScheduledTasksNotProcessedException extends Exception {

  public ScheduledTasksNotProcessedException(String msg) {
    super(msg);
  }
}
