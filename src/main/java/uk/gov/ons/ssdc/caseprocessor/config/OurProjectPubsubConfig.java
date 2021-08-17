package uk.gov.ons.ssdc.caseprocessor.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
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

  private final GcpPubSubProperties gcpPubSubProperties;

  public OurProjectPubsubConfig(GcpPubSubProperties gcpPubSubProperties) {
    this.gcpPubSubProperties = gcpPubSubProperties;
  }

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
  public DefaultPublisherFactory publisherFactory(
      CredentialsProvider credentialsProvider,
      @Qualifier("publisherTransportChannelProvider")
          TransportChannelProvider transportChannelProvider) {
    final DefaultPublisherFactory defaultPublisherFactory =
        new DefaultPublisherFactory(() -> ourPubsubProject);

    if (gcpPubSubProperties.getEmulatorHost() == null
        || "false".equals(gcpPubSubProperties.getEmulatorHost())) {
      defaultPublisherFactory.setCredentialsProvider(credentialsProvider);
    } else {
      // Since we cannot create a general NoCredentialsProvider if the emulator host is enabled
      // (because it would also be used for the other components), we have to create one here
      // for this particular case.
      defaultPublisherFactory.setCredentialsProvider(NoCredentialsProvider.create());
    }

    defaultPublisherFactory.setChannelProvider(transportChannelProvider);
    return defaultPublisherFactory;
  }

  @Bean("ourProjectSubscriberFactory")
  public DefaultSubscriberFactory subscriberFactory(
      CredentialsProvider credentialsProvider,
      @Qualifier("subscriberTransportChannelProvider")
          TransportChannelProvider transportChannelProvider) {
    final DefaultSubscriberFactory defaultSubscriberFactory =
        new DefaultSubscriberFactory(() -> ourPubsubProject);

    if (gcpPubSubProperties.getEmulatorHost() == null
        || "false".equals(gcpPubSubProperties.getEmulatorHost())) {
      defaultSubscriberFactory.setCredentialsProvider(credentialsProvider);
    } else {
      // Since we cannot create a general NoCredentialsProvider if the emulator host is enabled
      // (because it would also be used for the other components), we have to create one here
      // for this particular case.
      defaultSubscriberFactory.setCredentialsProvider(NoCredentialsProvider.create());
    }

    defaultSubscriberFactory.setCredentialsProvider(NoCredentialsProvider.create());
    defaultSubscriberFactory.setChannelProvider(transportChannelProvider);
    return defaultSubscriberFactory;
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
