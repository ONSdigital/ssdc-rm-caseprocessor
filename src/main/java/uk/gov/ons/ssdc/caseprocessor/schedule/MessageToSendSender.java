package uk.gov.ons.ssdc.caseprocessor.schedule;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import uk.gov.ons.ssdc.caseprocessor.model.entity.MessageToSend;

@Component
public class MessageToSendSender {
  private final PubSubTemplate pubSubTemplate;

  @Value("${queueconfig.publishtimeout}")
  private int publishTimeout;

  public MessageToSendSender(
      PubSubTemplate pubSubTemplate) {
    this.pubSubTemplate = pubSubTemplate;
  }

  public void sendMessage(MessageToSend messageToSend) {
    ListenableFuture<String> future =
        pubSubTemplate.publish(messageToSend.getDestinationTopic(), messageToSend.getMessageBody());

    try {
      future.get(publishTimeout, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
