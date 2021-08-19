package uk.gov.ons.ssdc.caseprocessor.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.SimplePubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"!test & !emulator"})
public class SharedProjectPubsubConfig {
  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  @Bean("sharedProjectPubSubSubscriberTemplate")
  public PubSubSubscriberTemplate pubSubSubscriberTemplate(
      @Qualifier("sharedProjectSubscriberFactory") SubscriberFactory subscriberFactory) {
    return new PubSubSubscriberTemplate(subscriberFactory);
  }

  @Bean("sharedProjectPubSubPublisherTemplate")
  public PubSubPublisherTemplate pubSubPublisherTemplate(
      @Qualifier("sharedProjectPublisherFactory") PublisherFactory publisherFactory) {
    return new PubSubPublisherTemplate(publisherFactory);
  }

  @Bean("sharedProjectPublisherFactory")
  public DefaultPublisherFactory publisherFactory() {
    return new DefaultPublisherFactory(() -> sharedPubsubProject);
  }

  @Bean("sharedProjectSubscriberFactory")
  public DefaultSubscriberFactory subscriberFactory() {
    return new DefaultSubscriberFactory(() -> sharedPubsubProject);
  }

  @Bean(name = "sharedProjectPubSubTemplate")
  public PubSubTemplate pubSubTemplate(
      @Qualifier("sharedProjectPubSubPublisherTemplate")
          PubSubPublisherTemplate pubSubPublisherTemplate,
      @Qualifier("sharedProjectPubSubSubscriberTemplate")
          PubSubSubscriberTemplate pubSubSubscriberTemplate,
      SimplePubSubMessageConverter simplePubSubMessageConverter) {
    PubSubTemplate pubSubTemplate =
        new PubSubTemplate(pubSubPublisherTemplate, pubSubSubscriberTemplate);
    pubSubTemplate.setMessageConverter(simplePubSubMessageConverter);
    return pubSubTemplate;
  }
}
