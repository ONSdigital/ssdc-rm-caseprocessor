package uk.gov.ons.ssdc.caseprocessor.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.MessageChannel;
import uk.gov.ons.ssdc.caseprocessor.messaging.ManagedMessageRecoverer;

@Configuration
public class MessageConsumerConfig {
  private final ManagedMessageRecoverer managedMessageRecoverer;

  @Value("${queueconfig.sample-subscription}")
  private String sampleSubscription;

  @Value("${queueconfig.print-fulfilment-subscription}")
  private String printFulfilmentSubscription;

  @Value("${queueconfig.receipt-subscription}")
  private String receiptSubscription;

  @Value("${queueconfig.refusal-subscription}")
  private String refusalSubscription;

  @Value("${queueconfig.invalid-case-subscription}")
  private String invalidCaseSubscription;

  @Value("${queueconfig.survey-launch-subscription}")
  private String surveyLaunchSubscription;

  @Value("${queueconfig.uac-authentication-subscription}")
  private String uacAuthenticationSubscription;

  @Value("${queueconfig.telephone-capture-subscription}")
  private String telephoneCaptureSubscription;

  @Value("${queueconfig.deactivate-uac-subscription}")
  private String deactivateUacSubscription;

  @Value("${queueconfig.update-sample-sensitive-subscription}")
  private String updateSampleSensitiveSubscription;

  @Value("${queueconfig.sms-fulfilment-subscription}")
  private String smsFulfilmentSubscription;

  public MessageConsumerConfig(ManagedMessageRecoverer managedMessageRecoverer) {
    this.managedMessageRecoverer = managedMessageRecoverer;
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
  public MessageChannel invalidCaseInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel surveyLaunchInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel uacAuthenticationInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel printFulfilmentInputChannel() {
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
  public MessageChannel smsFulfilmentInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter inboundSamples(
      @Qualifier("sampleInputChannel") MessageChannel channel,
      @Qualifier("ourProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, sampleSubscription);
  }

  @Bean
  public PubSubInboundChannelAdapter receiptInbound(
      @Qualifier("receiptInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, receiptSubscription);
  }

  @Bean
  public PubSubInboundChannelAdapter refusalInbound(
      @Qualifier("refusalInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, refusalSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter invalidCaseInbound(
      @Qualifier("invalidCaseInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, invalidCaseSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter surveyLaunchedInbound(
      @Qualifier("surveyLaunchInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, surveyLaunchSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter uacAuthenticationInbound(
      @Qualifier("uacAuthenticationInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, uacAuthenticationSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter fulfilmentInbound(
      @Qualifier("printFulfilmentInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, printFulfilmentSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter telephoneCaptureInbound(
      @Qualifier("telephoneCaptureInputChannel") MessageChannel channel,
      @Qualifier("ourProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, telephoneCaptureSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter deactivateUacInbound(
      @Qualifier("deactivateUacInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, deactivateUacSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter updateSampleSensitiveInbound(
      @Qualifier("updateSampleSensitiveInputChannel") MessageChannel channel,
      @Qualifier("sharedProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, updateSampleSensitiveSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter smsFulfilmentInbound(
      @Qualifier("smsFulfilmentInputChannel") MessageChannel channel,
      @Qualifier("ourProjectPubSubTemplate") PubSubTemplate pubSubTemplate) {
    return makeAdapter(channel, pubSubTemplate, smsFulfilmentSubscription);
  }

  private PubSubInboundChannelAdapter makeAdapter(
      MessageChannel channel, PubSubTemplate pubSubTemplate, String subscriptionName) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
    adapter.setOutputChannel(channel);
    adapter.setAckMode(AckMode.AUTO);
    return adapter;
  }

  @Bean
  public RequestHandlerRetryAdvice retryAdvice() {
    RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
    requestHandlerRetryAdvice.setRecoveryCallback(managedMessageRecoverer);
    return requestHandlerRetryAdvice;
  }
}
