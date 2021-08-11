package uk.gov.ons.ssdc.caseprocessor.testutils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper;

public class MessageConstructor {
  public static Message<byte[]> constructMessageWithValidTimeStamp(Object payload) {
    byte[] payloadBytes = JsonHelper.convertObjectToJson(payload).getBytes();
    return constructMessageInternal(payloadBytes);
  }

  private static <T> Message<T> constructMessageInternal(T msgPayload) {
    Message<T> message = mock(Message.class);
    when(message.getPayload()).thenReturn(msgPayload);

    // Now the timestamp fun.  Get one an hour+ ago, to 'prove' no fluking
    long timeStamp = OffsetDateTime.now().minusSeconds(3911).toInstant().toEpochMilli();

    MessageHeaders messageHeaders = mock(MessageHeaders.class);
    when(message.getHeaders()).thenReturn(messageHeaders);

    when(messageHeaders.getTimestamp()).thenReturn(timeStamp);

    return message;
  }
}
