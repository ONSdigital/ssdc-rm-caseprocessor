package uk.gov.ons.ssdc.caseprocessor.config;

import java.util.HashMap;
import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

@Configuration
public class MessageConsumerConfig {
  @Value("${queueconfig.sample-queue}")
  private String sampleQueue;

  @Bean
  public MessageChannel sampleInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public JmsMessageDrivenEndpoint sampleInput(
      AbstractMessageListenerContainer container, ChannelPublishingJmsMessageListener listener) {
    JmsMessageDrivenEndpoint endpoint = new JmsMessageDrivenEndpoint(container, listener);
    return endpoint;
  }

  @Bean
  public AbstractMessageListenerContainer sampleContainer(ConnectionFactory connectionFactory) {
    DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setDestinationName(sampleQueue);
    return container;
  }

  @Bean
  public ChannelPublishingJmsMessageListener sampleListener(MessageChannel sampleInputChannel) {
    ChannelPublishingJmsMessageListener listener = new ChannelPublishingJmsMessageListener();
    listener.setRequestChannel(sampleInputChannel);
    listener.setHeaderMapper(
        new JmsHeaderMapper() {

          @Override
          public void fromHeaders(MessageHeaders headers, Message target) {}

          @Override
          public Map<String, Object> toHeaders(Message source) {
            Map<String, Object> headers = new HashMap<>();

            /* We strip EVERYTHING out of the headers and put the content type back in because we
             * don't want the __TYPE__ to be processed by Spring Boot, which would cause
             * ClassNotFoundException because the type which was sent doesn't match the type we
             * want to receive. This is an ugly workaround, but it works.
             */
            headers.put("contentType", "application/json");
            return headers;
          }
        });
    return listener;
  }
}
