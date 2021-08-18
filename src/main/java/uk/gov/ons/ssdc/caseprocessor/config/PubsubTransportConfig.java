package uk.gov.ons.ssdc.caseprocessor.config;

import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.stub.PublisherStubSettings;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.threeten.bp.Duration;

@Configuration
@Profile({"!test"})
public class PubsubTransportConfig {
  @Bean
  @ConditionalOnMissingBean(name = "subscriberTransportChannelProvider")
  public TransportChannelProvider subscriberTransportChannelProvider() {
    return SubscriberStubSettings.defaultGrpcTransportProviderBuilder()
        .setKeepAliveTime(Duration.ofMinutes(5))
        .build();
  }

  @Bean
  @ConditionalOnMissingBean(name = "publisherTransportChannelProvider")
  public TransportChannelProvider publisherTransportChannelProvider() {
    return PublisherStubSettings.defaultGrpcTransportProviderBuilder()
        .setKeepAliveTime(Duration.ofMinutes(5))
        .build();
  }
}
