package uk.gov.ons.ssdc.caseprocessor.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Subscriber;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@Component
@ActiveProfiles("test")
@EnableRetry
public class PubsubHelper {
  @Autowired private PubSubTemplate pubSubTemplate;

  @Value("${spring.cloud.gcp.pubsub.emulator-host}")
  private String pubsubEmulatorHost;

  @Value("${spring.cloud.gcp.pubsub.project-id}")
  private String pubsubProjectId;

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  public <T> QueueSpy listen(String subscription, Class<T> contentClass) {
    BlockingQueue<T> queue = new ArrayBlockingQueue(50);
    Subscriber subscriber =
        pubSubTemplate.subscribe(
            subscription,
            message -> {
              try {
                T messageObject =
                    objectMapper.readValue(
                        message.getPubsubMessage().getData().toByteArray(), contentClass);
                queue.add(messageObject);
                message.ack();
              } catch (IOException e) {
                System.out.println("ERROR: Cannot unmarshal bad data on PubSub subscription");
              } finally {
                // Always want to ack, to get rid of dodgy messages
                message.ack();
              }
            });

    return new QueueSpy(queue, subscriber);
  }

  @Retryable(
      value = {java.io.IOException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 5000))
  public void sendMessage(String topicName, Object message) {
    ListenableFuture<String> future = pubSubTemplate.publish(topicName, message);

    try {
      future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public void purgeMessages(String subscription, String topic) {
    RestTemplate restTemplate = new RestTemplate();

    String subscriptionUrl =
        "http://"
            + pubsubEmulatorHost
            + "/v1/projects/"
            + pubsubProjectId
            + "/subscriptions/"
            + subscription;

    try {
      // There's no concept of a 'purge' with pubsub. Crudely, we have to delete & recreate
      restTemplate.delete(subscriptionUrl);
    } catch (HttpClientErrorException exception) {
      if (exception.getRawStatusCode() != 404) {
        throw exception;
      }
    }

    try {
      restTemplate.put(
          subscriptionUrl,
          new SubscriptionTopic("projects/" + pubsubProjectId + "/topics/" + topic));
    } catch (HttpClientErrorException exception) {
      if (exception.getRawStatusCode() != 409) {
        throw exception;
      }
    }
  }

  @Data
  @AllArgsConstructor
  private class SubscriptionTopic {
    private String topic;
  }
}
