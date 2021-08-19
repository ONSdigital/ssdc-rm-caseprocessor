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
public class OurProjectPubsubConfig {
  @Value("${queueconfig.our-pubsub-project}")
  private String ourPubsubProject;

  @Bean("ourProjectPubSubSubscriberTemplate")
  public PubSubSubscriberTemplate pubSubSubscriberTemplate(
      @Qualifier("ourProjectSubscriberFactory") SubscriberFactory subscriberFactory) {
    return new PubSubSubscriberTemplate(subscriberFactory);
  }

  @Bean("ourProjectPubSubPublisherTemplate")
  public PubSubPublisherTemplate pubSubPublisherTemplate(
      @Qualifier("ourProjectPublisherFactory") PublisherFactory publisherFactory) {
    return new PubSubPublisherTemplate(publisherFactory);
  }

  @Bean("ourProjectPublisherFactory")
  public PublisherFactory publisherFactory() {
    return new DefaultPublisherFactory(() -> ourPubsubProject);
  }

  @Bean("ourProjectSubscriberFactory")
  public SubscriberFactory subscriberFactory() {
    return new DefaultSubscriberFactory(() -> ourPubsubProject);
  }

  @Bean(name = "ourProjectPubSubTemplate")
  public PubSubTemplate pubSubTemplate(
      @Qualifier("ourProjectPubSubPublisherTemplate")
          PubSubPublisherTemplate pubSubPublisherTemplate,
      @Qualifier("ourProjectPubSubSubscriberTemplate")
          PubSubSubscriberTemplate pubSubSubscriberTemplate,
      SimplePubSubMessageConverter simplePubSubMessageConverter) {
    PubSubTemplate pubSubTemplate =
        new PubSubTemplate(pubSubPublisherTemplate, pubSubSubscriberTemplate);
    pubSubTemplate.setMessageConverter(simplePubSubMessageConverter);
    return pubSubTemplate;
  }
}
