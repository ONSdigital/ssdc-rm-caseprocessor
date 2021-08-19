package uk.gov.ons.ssdc.caseprocessor.testutils;

import static org.springframework.cloud.gcp.pubsub.support.PubSubSubscriptionUtils.toProjectSubscriptionName;
import static org.springframework.cloud.gcp.pubsub.support.PubSubTopicUtils.toProjectTopicName;
import static uk.gov.ons.ssdc.caseprocessor.testutils.TestConstants.OUR_PUBSUB_PROJECT;

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
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
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
  @Qualifier("pubSubTemplateForIntegrationTests")
  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Autowired private GcpPubSubProperties gcpPubSubProperties;

  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  public <T> QueueSpy sharedProjectListen(String subscription, Class<T> contentClass) {
    String fullyQualifiedSubscription =
        toProjectSubscriptionName(subscription, sharedPubsubProject).toString();
    return listen(fullyQualifiedSubscription, contentClass);
  }

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

  public void sendMessageToSharedProject(String topicName, Object message) {
    String fullyQualifiedTopic = toProjectTopicName(topicName, sharedPubsubProject).toString();
    sendMessage(fullyQualifiedTopic, message);
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
    purgeMessages(subscription, topic, OUR_PUBSUB_PROJECT);
  }

  public void purgeSharedProjectMessages(String subscription, String topic) {
    purgeMessages(subscription, topic, sharedPubsubProject);
  }

  private void purgeMessages(String subscription, String topic, String project) {
    RestTemplate restTemplate = new RestTemplate();

    String subscriptionUrl =
        "http://"
            + gcpPubSubProperties.getEmulatorHost()
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
