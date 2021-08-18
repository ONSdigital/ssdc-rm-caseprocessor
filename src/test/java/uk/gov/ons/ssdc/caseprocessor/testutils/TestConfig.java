package uk.gov.ons.ssdc.caseprocessor.testutils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.ssdc.caseprocessor.utils.ObjectMapperFactory;

@Configuration
@ActiveProfiles("test")
public class TestConfig {

  @Bean("ourProjectPubSubPublisherTemplateForIntegrationTests")
  public PubSubPublisherTemplate ourProjectPubSubPublisherTemplateForIntegrationTests(
      @Qualifier("ourProjectPublisherFactory") PublisherFactory publisherFactory) {
    PubSubPublisherTemplate publisherTemplate = new PubSubPublisherTemplate(publisherFactory);
    return publisherTemplate;
  }

  @Bean("sharedProjectPubSubPublisherTemplateForIntegrationTests")
  public PubSubPublisherTemplate sharedProjectPubSubPublisherTemplateForIntegrationTests(
      @Qualifier("sharedProjectPublisherFactory") PublisherFactory publisherFactory) {
    PubSubPublisherTemplate publisherTemplate = new PubSubPublisherTemplate(publisherFactory);
    return publisherTemplate;
  }

  @Bean("sharedProjectPubSubTemplateForIntegrationTests")
  public PubSubTemplate sharedProjectPubSubTemplateForIntegrationTests(
      @Qualifier("sharedProjectPubSubPublisherTemplateForIntegrationTests")
          PubSubPublisherTemplate pubSubPublisherTemplate,
      @Qualifier("sharedProjectPubSubSubscriberTemplate")
          PubSubSubscriberTemplate pubSubSubscriberTemplate,
      JacksonPubSubMessageConverter jacksonPubSubMessageConverterForIntegrationTests) {
    PubSubTemplate pubSubTemplate =
        new PubSubTemplate(pubSubPublisherTemplate, pubSubSubscriberTemplate);
    pubSubTemplate.setMessageConverter(jacksonPubSubMessageConverterForIntegrationTests);
    return pubSubTemplate;
  }

  @Bean("ourProjectPubSubTemplateForIntegrationTests")
  public PubSubTemplate ourProjectPubSubTemplateForIntegrationTests(
      @Qualifier("ourProjectPubSubPublisherTemplateForIntegrationTests")
          PubSubPublisherTemplate pubSubPublisherTemplate,
      @Qualifier("ourProjectPubSubSubscriberTemplate")
          PubSubSubscriberTemplate pubSubSubscriberTemplate,
      JacksonPubSubMessageConverter jacksonPubSubMessageConverterForIntegrationTests) {
    PubSubTemplate pubSubTemplate =
        new PubSubTemplate(pubSubPublisherTemplate, pubSubSubscriberTemplate);
    pubSubTemplate.setMessageConverter(jacksonPubSubMessageConverterForIntegrationTests);
    return pubSubTemplate;
  }

  @Bean
  public JacksonPubSubMessageConverter jacksonPubSubMessageConverterForIntegrationTests() {
    return new JacksonPubSubMessageConverter(ObjectMapperFactory.objectMapper());
  }
}
