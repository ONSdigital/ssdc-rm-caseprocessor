package uk.gov.ons.ssdc.caseprocessor.testutils;

import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@Configuration
@ActiveProfiles("test")
public class TestConfig {
  @Bean("pubSubTemplateForIntegrationTests")
  public PubSubTemplate pubSubTemplateForIntegrationTests(
      PublisherFactory publisherFactory,
      SubscriberFactory subscriberFactory,
      JacksonPubSubMessageConverter jacksonPubSubMessageConverterForIntegrationTests) {
    PubSubTemplate pubSubTemplate = new PubSubTemplate(publisherFactory, subscriberFactory);
    pubSubTemplate.setMessageConverter(jacksonPubSubMessageConverterForIntegrationTests);
    return pubSubTemplate;
  }

  @Bean
  public JacksonPubSubMessageConverter jacksonPubSubMessageConverterForIntegrationTests() {
    return new JacksonPubSubMessageConverter(ObjectMapperFactory.objectMapper());
  }
}
