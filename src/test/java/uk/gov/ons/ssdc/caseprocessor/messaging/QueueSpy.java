package uk.gov.ons.ssdc.caseprocessor.messaging;

import java.util.concurrent.BlockingQueue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

@AllArgsConstructor
public class QueueSpy implements AutoCloseable {
  @Getter private BlockingQueue<String> queue;
  private SimpleMessageListenerContainer container;

  @Override
  public void close() throws Exception {
    container.stop();
  }
}
