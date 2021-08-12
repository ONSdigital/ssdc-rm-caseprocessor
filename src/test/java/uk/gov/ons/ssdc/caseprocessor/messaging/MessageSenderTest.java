package uk.gov.ons.ssdc.caseprocessor.messaging;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CollectionCase;
import uk.gov.ons.ssdc.caseprocessor.model.entity.MessageToSend;
import uk.gov.ons.ssdc.caseprocessor.model.repository.MessageToSendRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper;

@ExtendWith(MockitoExtension.class)
public class MessageSenderTest {

  @Mock MessageToSendRepository messageToSendRepository;

  @InjectMocks MessageSender underTest;

  @Test
  public void testSendMessage() {
    String destinationTopic = "test-topic";
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setCaseId(UUID.randomUUID());

    underTest.sendMessage(destinationTopic, collectionCase);

    ArgumentCaptor<MessageToSend> messageToSendArgumentCaptor =
        ArgumentCaptor.forClass(MessageToSend.class);
    verify(messageToSendRepository).saveAndFlush(messageToSendArgumentCaptor.capture());

    MessageToSend actualMessageSent = messageToSendArgumentCaptor.getValue();
    assertThat(actualMessageSent.getDestinationTopic()).isEqualTo(destinationTopic);
    assertThat(actualMessageSent.getMessageBody())
        .isEqualTo(JsonHelper.convertObjectToJson(collectionCase));
  }
}
