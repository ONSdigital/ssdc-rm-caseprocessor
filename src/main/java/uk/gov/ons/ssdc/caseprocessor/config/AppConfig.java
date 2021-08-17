package uk.gov.ons.ssdc.caseprocessor.config;

import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
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
