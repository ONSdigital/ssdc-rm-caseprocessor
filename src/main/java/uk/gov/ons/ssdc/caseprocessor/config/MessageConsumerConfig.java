package uk.gov.ons.ssdc.caseprocessor.config;

import static com.google.cloud.spring.pubsub.support.PubSubSubscriptionUtils.toProjectSubscriptionName;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryListener;
import uk.gov.ons.ssdc.caseprocessor.messaging.ManagedMessageRecoverer;

@Configuration
public class MessageConsumerConfig {
  private final ManagedMessageRecoverer managedMessageRecoverer;
  private final PubSubTemplate pubSubTemplate;

  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  @Value("${queueconfig.new-case-subscription}")
  private String newCaseSubscription;

  @Value("${queueconfig.print-fulfilment-subscription}")
  private String printFulfilmentSubscription;

  @Value("${queueconfig.receipt-subscription}")
  private String receiptSubscription;

  @Value("${queueconfig.refusal-subscription}")
  private String refusalSubscription;

  @Value("${queueconfig.invalid-case-subscription}")
  private String invalidCaseSubscription;

  @Value("${queueconfig.eq-launch-subscription}")
  private String eqLaunchSubscription;

  @Value("${queueconfig.deactivate-uac-subscription}")
  private String deactivateUacSubscription;

  @Value("${queueconfig.update-sample-subscription}")
  private String updateSampleSubscription;

  @Value("${queueconfig.update-sample-sensitive-subscription}")
  private String updateSampleSensitiveSubscription;

  @Value("${queueconfig.sms-confirmation-subscription}")
  private String smsConfirmationSubscription;

  @Value("${queueconfig.email-confirmation-subscription}")
  private String emailConfirmationSubscription;

  public MessageConsumerConfig(
      ManagedMessageRecoverer managedMessageRecoverer, PubSubTemplate pubSubTemplate) {
    this.managedMessageRecoverer = managedMessageRecoverer;
    this.pubSubTemplate = pubSubTemplate;
  }

  @Bean
  public MessageChannel newCaseInputChannel() {
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
  public MessageChannel eqLaunchInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel printFulfilmentInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel deactivateUacInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel updateSampleInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel updateSampleSensitiveInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel smsConfirmationInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel emailConfirmationInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter newCaseInbound(
      @Qualifier("newCaseInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(newCaseSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  public PubSubInboundChannelAdapter receiptInbound(
      @Qualifier("receiptInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(receiptSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  public PubSubInboundChannelAdapter refusalInbound(
      @Qualifier("refusalInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(refusalSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  PubSubInboundChannelAdapter invalidCaseInbound(
      @Qualifier("invalidCaseInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(invalidCaseSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  PubSubInboundChannelAdapter eqLaunchedInbound(
      @Qualifier("eqLaunchInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(eqLaunchSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  PubSubInboundChannelAdapter fulfilmentInbound(
      @Qualifier("printFulfilmentInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(printFulfilmentSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  PubSubInboundChannelAdapter deactivateUacInbound(
      @Qualifier("deactivateUacInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(deactivateUacSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  PubSubInboundChannelAdapter updateSampleInbound(
      @Qualifier("updateSampleInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(updateSampleSubscription, sharedPubsubProject).toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  PubSubInboundChannelAdapter updateSampleSensitiveInbound(
      @Qualifier("updateSampleSensitiveInputChannel") MessageChannel channel) {
    String subscription =
        toProjectSubscriptionName(updateSampleSensitiveSubscription, sharedPubsubProject)
            .toString();
    return makeAdapter(channel, subscription);
  }

  @Bean
  PubSubInboundChannelAdapter smsConfirmationInbound(
      @Qualifier("smsConfirmationInputChannel") MessageChannel channel) {
    return makeAdapter(channel, smsConfirmationSubscription);
  }

  @Bean
  PubSubInboundChannelAdapter emailConfirmationInbound(
      @Qualifier("emailConfirmationInputChannel") MessageChannel channel) {
    return makeAdapter(channel, emailConfirmationSubscription);
  }

  private PubSubInboundChannelAdapter makeAdapter(MessageChannel channel, String subscriptionName) {
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

  @Bean
  public RetryListener retryListener() {
    RetryListener retryListener = new DefaultListenerSupport();

    return retryListener;
  }
}
