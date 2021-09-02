package uk.gov.ons.ssdc.caseprocessor.messaging;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.repository.MessageToSendRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper;
import uk.gov.ons.ssdc.common.model.entity.MessageToSend;

@Component
public class MessageSender {
  private final MessageToSendRepository messageToSendRepository;

  public MessageSender(MessageToSendRepository messageToSendRepository) {
    this.messageToSendRepository = messageToSendRepository;
  }

  public void sendMessage(String destinationTopic, Object message) {
    MessageToSend messageToSend = new MessageToSend();
    messageToSend.setId(UUID.randomUUID());
    messageToSend.setDestinationTopic(destinationTopic);
    messageToSend.setMessageBody(JsonHelper.convertObjectToJson(message));

    messageToSendRepository.save(messageToSend);
  }
}
