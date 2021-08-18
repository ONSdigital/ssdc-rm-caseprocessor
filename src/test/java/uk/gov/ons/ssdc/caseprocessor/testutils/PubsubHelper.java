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
import org.springframework.beans.factory.annotation.Qualifier;
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
  @Autowired
  @Qualifier("sharedProjectPubSubTemplateForIntegrationTests")
  private PubSubTemplate sharedProjectPubSubTemplate;

  @Autowired
  @Qualifier("ourProjectPubSubTemplateForIntegrationTests")
  private PubSubTemplate ourProjectPubSubTemplate;

  @Value("${spring.cloud.gcp.pubsub.emulator-host}")
  private String pubsubEmulatorHost;

  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  @Value("${queueconfig.our-pubsub-project}")
  private String ourPubsubProject;

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  public <T> QueueSpy listen(String subscription, Class<T> contentClass) {
    return listen(subscription, contentClass, false);
  }

  public <T> QueueSpy listen(
      String subscription, Class<T> contentClass, boolean receiveFromOurProject) {
    if (receiveFromOurProject) {
      return listen(subscription, contentClass, ourProjectPubSubTemplate);
    } else {
      return listen(subscription, contentClass, sharedProjectPubSubTemplate);
    }
  }

  private <T> QueueSpy listen(String subscription, Class<T> contentClass, PubSubTemplate template) {
    BlockingQueue<T> queue = new ArrayBlockingQueue(50);
    Subscriber subscriber =
        template.subscribe(
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

  public void sendMessage(String topicName, Object message) {
    sendMessage(topicName, message, false);
  }

  public void sendMessage(String topicName, Object message, boolean sendToOurProject) {
    if (sendToOurProject) {
      sendMessage(topicName, message, ourProjectPubSubTemplate);
    } else {
      sendMessage(topicName, message, sharedProjectPubSubTemplate);
    }
  }

  @Retryable(
      value = {java.io.IOException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 5000))
  private void sendMessage(String topicName, Object message, PubSubTemplate template) {
    ListenableFuture<String> future = template.publish(topicName, message);

    try {
      future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public void purgeMessages(String subscription, String topic) {
    purgeMessages(subscription, topic, false);
  }

  public void purgeMessages(String subscription, String topic, boolean ourProject) {
    if (ourProject) {
      purgeMessages(subscription, topic, ourPubsubProject);
    } else {
      purgeMessages(subscription, topic, sharedPubsubProject);
    }
  }

  private void purgeMessages(String subscription, String topic, String project) {
    RestTemplate restTemplate = new RestTemplate();

    String subscriptionUrl =
        "http://"
            + pubsubEmulatorHost
            + "/v1/projects/"
            + project
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
          subscriptionUrl, new SubscriptionTopic("projects/" + project + "/topics/" + topic));
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
