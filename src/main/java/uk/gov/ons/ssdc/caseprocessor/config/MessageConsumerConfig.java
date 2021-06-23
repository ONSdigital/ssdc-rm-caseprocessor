package uk.gov.ons.ssdc.caseprocessor.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import uk.gov.ons.ssdc.caseprocessor.client.ExceptionManagerClient;
import uk.gov.ons.ssdc.caseprocessor.messaging.ManagedMessageRecoverer;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;

@Configuration
public class MessageConsumerConfig {
  private final ExceptionManagerClient exceptionManagerClient;
  private final ConnectionFactory connectionFactory;

  @Value("${queueconfig.sample-queue}")
  private String sampleQueue;

  @Value("${queueconfig.consumers}")
  private int consumers;

  @Value("${queueconfig.retry-attempts}")
  private int retryAttempts;

  @Value("${queueconfig.retry-delay}")
  private int retryDelay;

  @Value("${messagelogging.logstacktraces}")
  private boolean logStackTraces;

  @Value("${queueconfig.receipt-response-queue}")
  private String receiptQueue;

  @Value("${queueconfig.refusal-response-queue}")
  private String refusalQueue;

  @Value("${queueconfig.invalid-address-queue}")
  private String invalidAddressQueue;

  @Value("${queueconfig.survey-launched-queue}")
  private String surveyLaunchedQueue;

  @Value("${queueconfig.telephone-capture-queue}")
  private String telephoneCaptureQueue;

  public MessageConsumerConfig(
      ExceptionManagerClient exceptionManagerClient, ConnectionFactory connectionFactory) {
    this.exceptionManagerClient = exceptionManagerClient;
    this.connectionFactory = connectionFactory;
  }

  @Bean
  public MessageChannel sampleInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel receiptInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel refusalInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel invalidAddressInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel surveyLaunchedInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel telephoneCaptureInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public AmqpInboundChannelAdapter inboundSamples(
      @Qualifier("sampleContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("sampleInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  public AmqpInboundChannelAdapter receiptInbound(
      @Qualifier("receiptContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("receiptInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  public AmqpInboundChannelAdapter refusalInbound(
      @Qualifier("refusalContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("refusalInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  AmqpInboundChannelAdapter invalidAddressInbound(
      @Qualifier("invalidAddressContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("invalidAddressInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  AmqpInboundChannelAdapter surveyLaunchedInbound(
      @Qualifier("surveyLaunchedContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("surveyLaunchedInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  AmqpInboundChannelAdapter telephoneCaptureInbound(
      @Qualifier("telephoneCaptureContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("telephoneCaptureInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  public SimpleMessageListenerContainer surveyLaunchedContainer() {
    return setupListenerContainer(surveyLaunchedQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer sampleContainer() {
    return setupListenerContainer(sampleQueue, Sample.class);
  }

  @Bean
  public SimpleMessageListenerContainer receiptContainer() {
    return setupListenerContainer(receiptQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer refusalContainer() {
    return setupListenerContainer(refusalQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer invalidAddressContainer() {
    return setupListenerContainer(invalidAddressQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer telephoneCaptureContainer() {
    return setupListenerContainer(telephoneCaptureQueue, ResponseManagementEvent.class);
  }

  private SimpleMessageListenerContainer setupListenerContainer(
      String queueName, Class expectedMessageType) {
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(retryDelay);

    ManagedMessageRecoverer managedMessageRecoverer =
        new ManagedMessageRecoverer(
            exceptionManagerClient,
            expectedMessageType,
            logStackTraces,
            "Case Processor",
            queueName);

    RetryOperationsInterceptor retryOperationsInterceptor =
        RetryInterceptorBuilder.stateless()
            .maxAttempts(retryAttempts)
            .backOffPolicy(fixedBackOffPolicy)
            .recoverer(managedMessageRecoverer)
            .build();

    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
    container.setQueueNames(queueName);
    container.setConcurrentConsumers(consumers);
    container.setAdviceChain(retryOperationsInterceptor);
    return container;
  }

  private AmqpInboundChannelAdapter makeAdapter(
      AbstractMessageListenerContainer listenerContainer, MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    adapter.setHeaderMapper(
        new DefaultAmqpHeaderMapper(null, null) {
          @Override
          public Map<String, Object> toHeadersFromRequest(MessageProperties source) {
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
    return adapter;
  }
}
