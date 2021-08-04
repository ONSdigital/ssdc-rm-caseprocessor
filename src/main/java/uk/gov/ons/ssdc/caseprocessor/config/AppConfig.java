package uk.gov.ons.ssdc.caseprocessor.config;

import java.util.TimeZone;
import javax.annotation.PostConstruct;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@Configuration
@EnableScheduling
public class AppConfig {
  @Bean
  public PubSubTemplate pubSubTemplate(
      PublisherFactory publisherFactory,
      SubscriberFactory subscriberFactory,
      JacksonPubSubMessageConverter jacksonPubSubMessageConverter) {
    PubSubTemplate pubSubTemplate = new PubSubTemplate(publisherFactory, subscriberFactory);
    pubSubTemplate.setMessageConverter(jacksonPubSubMessageConverter);
    return pubSubTemplate;
  }

  @Bean
  public JacksonPubSubMessageConverter messageConverter() {
    return new JacksonPubSubMessageConverter(ObjectMapperFactory.objectMapper());
  }

  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}
