package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.entity.MessageToSend;
import uk.gov.ons.ssdc.caseprocessor.model.repository.MessageToSendRepository;

@Component
public class MessageToSendProcessor {
  private static final Logger log = LoggerFactory.getLogger(MessageToSendProcessor.class);
  private final MessageToSendRepository messageToSendRepository;
  private final MessageToSendSender messageToSendSender;

  @Value("${scheduler.chunksize}")
  private int chunkSize;

  public MessageToSendProcessor(
      MessageToSendRepository messageToSendRepository, MessageToSendSender messageToSendSender) {
    this.messageToSendRepository = messageToSendRepository;
    this.messageToSendSender = messageToSendSender;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every chunk
  public void processChunk() {
    try (Stream<MessageToSend> messagesToSend =
        messageToSendRepository.findMessagesToSend(chunkSize)) {
      messagesToSend.forEach(
          messageToSend -> {
            try {
              messageToSendSender.sendMessage(messageToSend);
              messageToSendRepository.delete(messageToSend);
            } catch (Exception exception) {
              log.with(messageToSend)
                  .error("Could not send message. Will retry indefinitely", exception);
            }
          });
    }
  }

  @Transactional
  public boolean isThereWorkToDo() {
    return messageToSendRepository.count() > 0;
  }
}
