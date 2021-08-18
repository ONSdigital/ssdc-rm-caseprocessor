package uk.gov.ons.ssdc.caseprocessor.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
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

@Configuration
public class OurProjectPubsubConfig {
  @Value("${queueconfig.our-pubsub-project}")
  private String ourPubsubProject;

  @Value("${spring.cloud.gcp.pubsub.emulator-host}")
  private String pubsubEmulatorHost;

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
  public PublisherFactory publisherFactory(
      CredentialsProvider credentialsProvider,
      @Qualifier("publisherTransportChannelProvider")
          TransportChannelProvider transportChannelProvider) {
    DefaultPublisherFactory publisherFactory = new DefaultPublisherFactory(() -> ourPubsubProject);

    if (pubsubEmulatorHost == null || "false".equals(pubsubEmulatorHost)) {
      publisherFactory.setCredentialsProvider(credentialsProvider);
    } else {
      // Since we cannot create a general NoCredentialsProvider if the emulator host is enabled
      // (because it would also be used for the other components), we have to create one here
      // for this particular case.
      publisherFactory.setCredentialsProvider(NoCredentialsProvider.create());
    }

    publisherFactory.setChannelProvider(transportChannelProvider);
    return publisherFactory;
  }

  @Bean("ourProjectSubscriberFactory")
  public SubscriberFactory subscriberFactory(
      CredentialsProvider credentialsProvider,
      @Qualifier("subscriberTransportChannelProvider")
          TransportChannelProvider transportChannelProvider) {
    DefaultSubscriberFactory subscriberFactory =
        new DefaultSubscriberFactory(() -> ourPubsubProject);

    if (pubsubEmulatorHost == null || "false".equals(pubsubEmulatorHost)) {
      subscriberFactory.setCredentialsProvider(credentialsProvider);
    } else {
      // Since we cannot create a general NoCredentialsProvider if the emulator host is enabled
      // (because it would also be used for the other components), we have to create one here
      // for this particular case.
      subscriberFactory.setCredentialsProvider(NoCredentialsProvider.create());
    }

    subscriberFactory.setChannelProvider(transportChannelProvider);
    return subscriberFactory;
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
