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
public class SharedProjectPubsubConfig {
  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  private final GcpPubSubProperties gcpPubSubProperties;

  public SharedProjectPubsubConfig(
      GcpPubSubProperties gcpPubSubProperties) {
    this.gcpPubSubProperties = gcpPubSubProperties;
  }

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
  public DefaultPublisherFactory publisherFactory(CredentialsProvider credentialsProvider,
      @Qualifier("publisherTransportChannelProvider") TransportChannelProvider transportChannelProvider) {
    final DefaultPublisherFactory defaultPublisherFactory = new DefaultPublisherFactory(() -> sharedPubsubProject);

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

  @Bean("sharedProjectSubscriberFactory")
  public DefaultSubscriberFactory subscriberFactory(CredentialsProvider credentialsProvider,
      @Qualifier("subscriberTransportChannelProvider") TransportChannelProvider transportChannelProvider) {
    final DefaultSubscriberFactory defaultSubscriberFactory = new DefaultSubscriberFactory(() -> sharedPubsubProject);

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

  @Bean(name = "sharedProjectPubSubTemplate")
  public PubSubTemplate pubSubTemplate(
      PubSubPublisherTemplate pubSubPublisherTemplate,
      PubSubSubscriberTemplate pubSubSubscriberTemplate,
      SimplePubSubMessageConverter simplePubSubMessageConverter) {
    PubSubTemplate pubSubTemplate = new PubSubTemplate(pubSubPublisherTemplate, pubSubSubscriberTemplate);
    pubSubTemplate.setMessageConverter(simplePubSubMessageConverter);
    return pubSubTemplate;
  }
}
