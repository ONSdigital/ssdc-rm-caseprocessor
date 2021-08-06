package uk.gov.ons.ssdc.caseprocessor.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.protobuf.ByteString;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.logstash.logback.encoder.org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import uk.gov.ons.ssdc.caseprocessor.client.ExceptionManagerClient;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ExceptionReportResponse;
import uk.gov.ons.ssdc.caseprocessor.model.dto.SkippedMessage;

public class ManagedMessageRecoverer implements RecoveryCallback<Object> {
  private static final Logger log = LoggerFactory.getLogger(ManagedMessageRecoverer.class);
  private static final String SERVICE_NAME = "Case Processor";
  private static final MessageDigest digest;

  static {
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      log.error("Could not initialise hashing", e);
      throw new RuntimeException("Could not initialise hashing", e);
    }
  }

  private final ExceptionManagerClient exceptionManagerClient;

  public ManagedMessageRecoverer(ExceptionManagerClient exceptionManagerClient) {
    this.exceptionManagerClient = exceptionManagerClient;
  }

  @Override
  public Object recover(RetryContext retryContext) throws Exception {
    if (!(retryContext.getLastThrowable() instanceof MessagingException)) {
      log.error(
          "Super duper unexpected kind of error, so going to fail very noisily",
          retryContext.getLastThrowable());
      throw new RuntimeException(retryContext.getLastThrowable());
    }

    MessagingException messagingException = (MessagingException) retryContext.getLastThrowable();
    Message<?> message = messagingException.getFailedMessage();
    BasicAcknowledgeablePubsubMessage originalMessage =
        (BasicAcknowledgeablePubsubMessage) message.getHeaders().get("gcp_pubsub_original_message");
    String subscriptionName = originalMessage.getProjectSubscriptionName().getSubscription();
    ByteString originalMessageByteString = originalMessage.getPubsubMessage().getData();
    byte[] rawMessageBody = new byte[originalMessageByteString.size()];
    originalMessageByteString.copyTo(rawMessageBody, 0);
    String messageHash;

    // Digest is not thread-safe
    synchronized (digest) {
      messageHash = bytesToHexString(digest.digest(rawMessageBody));
    }

    String stackTraceRootCause = findUsefulRootCauseInStackTrace(retryContext.getLastThrowable());

    ExceptionReportResponse reportResult =
        getExceptionReportResponse(
            retryContext.getLastThrowable(), messageHash, stackTraceRootCause, subscriptionName);

    if (skipMessage(
        reportResult,
        messageHash,
        rawMessageBody,
        retryContext.getLastThrowable(),
        originalMessage,
        subscriptionName)) {
      return null; // Our work here is done
    }

    peekMessage(reportResult, messageHash, rawMessageBody);

    logMessage(
        reportResult,
        retryContext.getLastThrowable().getCause(),
        messageHash,
        rawMessageBody,
        stackTraceRootCause);

    // Reject the original message where it'll be retried at some future point in time
    originalMessage.nack();

    return null;
  }

  private ExceptionReportResponse getExceptionReportResponse(
      Throwable cause, String messageHash, String stackTraceRootCause, String subscriptionName) {
    ExceptionReportResponse reportResult = null;
    try {
      reportResult =
          exceptionManagerClient.reportException(
              messageHash, SERVICE_NAME, subscriptionName, cause, stackTraceRootCause);
    } catch (Exception exceptionManagerClientException) {
      log.with("reason", exceptionManagerClientException.getMessage())
          .warn(
              "Could not report to Exception Manager. There will be excessive logging until resolved");
    }
    return reportResult;
  }

  private boolean skipMessage(
      ExceptionReportResponse reportResult,
      String messageHash,
      byte[] rawMessageBody,
      Throwable cause,
      BasicAcknowledgeablePubsubMessage originalMessage,
      String subscriptionName) {

    if (reportResult == null || !reportResult.isSkipIt()) {
      return false;
    }

    boolean result = false;

    // Make certain that we have a copy of the message before quarantining it
    try {
      SkippedMessage skippedMessage = new SkippedMessage();
      skippedMessage.setMessageHash(messageHash);
      skippedMessage.setMessagePayload(rawMessageBody);
      skippedMessage.setService(SERVICE_NAME);
      skippedMessage.setQueue(subscriptionName);
      skippedMessage.setContentType("application/json");
      skippedMessage.setHeaders(null);
      skippedMessage.setRoutingKey(null);
      exceptionManagerClient.storeMessageBeforeSkipping(skippedMessage);
      result = true;
    } catch (Exception exceptionManagerClientException) {
      log.with("message_hash", messageHash)
          .warn(
              "Unable to store a copy of the message. Will NOT be quarantining",
              exceptionManagerClientException);
    }

    // If the quarantined message is persisted OK then we can ACK the message
    if (result) {
      log.with("message_hash", messageHash).warn("Quarantined message");
    }

    return result;
  }

  private void peekMessage(
      ExceptionReportResponse reportResult, String messageHash, byte[] rawMessageBody) {
    if (reportResult == null || !reportResult.isPeek()) {
      return;
    }

    try {
      // Send it back to the exception manager so it can be peeked
      exceptionManagerClient.respondToPeek(messageHash, rawMessageBody);
    } catch (Exception respondException) {
      // Nothing we can do about this - ignore it
    }
  }

  private void logMessage(
      ExceptionReportResponse reportResult,
      Throwable cause,
      String messageHash,
      byte[] rawMessageBody,
      String stackTraceRootCause) {
    if (reportResult != null && !reportResult.isLogIt()) {
      return;
    }

    if (false) { // TODO - config for whether to log stack traces or not
      log.with("message_hash", messageHash).error("Could not process message", cause);
    } else {
      log.with("message_hash", messageHash)
          .with("cause", cause.getMessage())
          .with("root_cause", stackTraceRootCause)
          .error("Could not process message");
    }
  }

  private String bytesToHexString(byte[] hash) {
    StringBuffer hexString = new StringBuffer();
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  private String findUsefulRootCauseInStackTrace(Throwable cause) {
    String[] stackTrace = ExceptionUtils.getRootCauseStackTrace(cause);

    // Iterate through the stack trace until we hit the first problem with our code
    for (String stackTraceLine : stackTrace) {
      if (stackTraceLine.contains("uk.gov.ons.census")) {
        return stackTraceLine;
      }
    }

    return stackTrace[0];
  }
}
