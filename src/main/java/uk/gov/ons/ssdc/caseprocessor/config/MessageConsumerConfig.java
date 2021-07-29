package uk.gov.ons.ssdc.caseprocessor.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.Sample;

@Configuration
public class MessageConsumerConfig {
  @Value("${queueconfig.sample-subscription}")
  private String sampleSubscription;

  @Value("${queueconfig.fulfilment-subscription}")
  private String fulfilmentSubscription;

  @Value("${queueconfig.receipt-response-subscription}")
  private String receiptSubscription;

  @Value("${queueconfig.refusal-response-subscription}")
  private String refusalSubscription;

  @Value("${queueconfig.invalid-address-subscription}")
  private String invalidAddressSubscription;

  @Value("${queueconfig.survey-launched-subscription}")
  private String surveyLaunchedSubscription;

  @Value("${queueconfig.telephone-capture-subscription}")
  private String telephoneCaptureSubscription;

  @Value("${queueconfig.deactivate-uac-subscription}")
  private String deactivateUacSubscription;

  @Value("${queueconfig.update-sample-sensitive-subscription}")
  private String updateSampleSensitiveSubscription;

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
  public MessageChannel fulfilmentInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel telephoneCaptureInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel deactivateUacInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel updateSampleSensitiveInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter inboundSamples(
      @Qualifier("sampleInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, sampleSubscription, Sample.class);
  }

  @Bean
  public PubSubInboundChannelAdapter receiptInbound(
      @Qualifier("receiptInputChannel") MessageChannel channel, PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, receiptSubscription, ResponseManagementEvent.class);
  }

  @Bean
  public PubSubInboundChannelAdapter refusalInbound(
      @Qualifier("refusalInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, refusalSubscription, ResponseManagementEvent.class);
  }

  @Bean
  PubSubInboundChannelAdapter invalidAddressInbound(
      @Qualifier("invalidAddressInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, invalidAddressSubscription, ResponseManagementEvent.class);
  }

  @Bean
  PubSubInboundChannelAdapter surveyLaunchedInbound(
      @Qualifier("surveyLaunchedInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, surveyLaunchedSubscription, ResponseManagementEvent.class);
  }

  @Bean
  PubSubInboundChannelAdapter fulfilmentInbound(
      @Qualifier("fulfilmentInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, fulfilmentSubscription, ResponseManagementEvent.class);
  }

  @Bean
  PubSubInboundChannelAdapter telephoneCaptureInbound(
      @Qualifier("telephoneCaptureInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, telephoneCaptureSubscription, ResponseManagementEvent.class);
  }

  @Bean
  PubSubInboundChannelAdapter deactivateUacInbound(
      @Qualifier("deactivateUacInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, deactivateUacSubscription, ResponseManagementEvent.class);
  }

  @Bean
  PubSubInboundChannelAdapter updateSampleSensitiveInbound(
      @Qualifier("updateSampleSensitiveInputChannel") MessageChannel channel,
      PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, updateSampleSensitiveSubscription, ResponseManagementEvent.class);
  }

  private PubSubInboundChannelAdapter makeAdapter(MessageChannel channel,
      PubSubTemplate pubSubTemplate, String subscriptionName, Class<?> payloadType) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
    adapter.setOutputChannel(channel);
    adapter.setAckMode(AckMode.MANUAL);
    adapter.setPayloadType(payloadType);
    return adapter;
  }
}
