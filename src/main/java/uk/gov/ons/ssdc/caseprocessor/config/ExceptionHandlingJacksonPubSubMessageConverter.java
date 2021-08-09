package uk.gov.ons.ssdc.caseprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.pubsub.v1.PubsubMessage;
import java.util.Map;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConversionException;

public class ExceptionHandlingJacksonPubSubMessageConverter extends JacksonPubSubMessageConverter {

  public ExceptionHandlingJacksonPubSubMessageConverter(
      ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public PubsubMessage toPubSubMessage(Object payload, Map<String, String> headers) {
    return super.toPubSubMessage(payload, headers);
  }

  @Override
  public <T> T fromPubSubMessage(PubsubMessage message, Class<T> payloadType) {
    try {
      return super.fromPubSubMessage(message, payloadType);
    } catch (PubSubMessageConversionException exception) {
      Map<String, String> attributesMap = message.getAttributesMap();
      System.out.println("Foobar");
      throw exception;
    }
  }
}
