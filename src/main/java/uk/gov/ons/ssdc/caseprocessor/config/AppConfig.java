package uk.gov.ons.ssdc.caseprocessor.config;

import java.util.TimeZone;
import javax.annotation.PostConstruct;
import org.springframework.cloud.gcp.pubsub.support.converter.SimplePubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {
  @Bean
  public SimplePubSubMessageConverter messageConverter() {
    return new SimplePubSubMessageConverter();
  }

  //  @Bean
  //  public TransportChannelProvider transportChannelProvider() {
  //    String hostport = System.getenv("PUBSUB_EMULATOR_HOST");
  //    ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
  //    TransportChannelProvider channelProvider =
  //        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
  //    return channelProvider;
  //  }

  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}
