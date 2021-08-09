package uk.gov.ons.ssdc.caseprocessor.messaging;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

public class WibbleNorbert extends DefaultErrorMessageStrategy {
  public ErrorMessage buildErrorMessage(Throwable throwable, @Nullable AttributeAccessor attributes) {
    Object inputMessage = attributes == null ? null : attributes.getAttribute("inputMessage");
    return inputMessage instanceof Message ? new ErrorMessage(throwable, (Message)inputMessage) : new ErrorMessage(throwable);
  }
}
