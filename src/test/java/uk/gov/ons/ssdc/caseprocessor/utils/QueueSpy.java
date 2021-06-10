package uk.gov.ons.ssdc.caseprocessor.utils;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;

@AllArgsConstructor
public class QueueSpy implements AutoCloseable {
  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @Getter private BlockingQueue<String> queue;
  private SimpleMessageListenerContainer container;

  @Override
  public void close() {
    container.stop();
  }

  public ResponseManagementEvent checkExpectedMessageReceived()
      throws IOException, InterruptedException {
    String actualMessage = queue.poll(20, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);
    ResponseManagementEvent responseManagementEvent =
        objectMapper.readValue(actualMessage, ResponseManagementEvent.class);
    assertNotNull(responseManagementEvent);
    assertEquals("RM", responseManagementEvent.getEvent().getChannel());
    return responseManagementEvent;
  }

  public void checkMessageIsNotReceived(int timeOut) throws InterruptedException {
    String actualMessage = queue.poll(timeOut, TimeUnit.SECONDS);
    assertNull("Message received when not expected", actualMessage);
  }
}
