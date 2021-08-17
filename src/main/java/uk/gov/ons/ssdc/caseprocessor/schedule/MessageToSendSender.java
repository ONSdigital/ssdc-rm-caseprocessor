package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import uk.gov.ons.ssdc.caseprocessor.model.entity.MessageToSend;

@Component
public class MessageToSendSender {
  private final PubSubTemplate sharedProjectPubSubTemplate;
  private final PubSubTemplate ourProjectPubSubTemplate;

  @Value("${queueconfig.publishtimeout}")
  private int publishTimeout;

  public MessageToSendSender(
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate sharedProjectPubSubTemplate,
      @Qualifier("ourProjectPubSubTemplate") PubSubTemplate ourProjectPubSubTemplate) {
    this.sharedProjectPubSubTemplate = sharedProjectPubSubTemplate;
    this.ourProjectPubSubTemplate = ourProjectPubSubTemplate;
  }

  public void sendMessage(MessageToSend messageToSend) {
    PubsubMessage pubsubMessage =
        PubsubMessage.newBuilder()
            .setData(ByteString.copyFromUtf8(messageToSend.getMessageBody()))
            .build();

    ListenableFuture<String> future;

    if (messageToSend.isSendToOurProject()) {
      future = ourProjectPubSubTemplate.publish(messageToSend.getDestinationTopic(), pubsubMessage);
    } else {
      future =
          sharedProjectPubSubTemplate.publish(messageToSend.getDestinationTopic(), pubsubMessage);
    }

    try {
      future.get(publishTimeout, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
