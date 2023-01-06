package uk.gov.ons.ssdc.caseprocessor.service;

import static uk.gov.ons.ssdc.caseprocessor.model.dto.CloudTaskType.EQ_FLUSH;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CloudTaskMessage;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EqFlushTaskPayload;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@Component
public class EqFlushProcessor {
  private final MessageSender messageSender;

  @Value("${queueconfig.cloud-task-queue-topic}")
  private String cloudTaskQueueTopic;

  public EqFlushProcessor(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  public void process(Case caze, ActionRule actionRule) {
    List<UacQidLink> uacQidLinks = caze.getUacQidLinks();

    for (UacQidLink uacQidLink : uacQidLinks) {
      if (uacQidLink.isEqLaunched() && !uacQidLink.isReceiptReceived()) {
        CloudTaskMessage cloudTaskMessage =
            prepareEqFlushCloudTaskMessage(uacQidLink, actionRule.getId());
        messageSender.sendMessage(cloudTaskQueueTopic, cloudTaskMessage);
      }
    }
  }

  private CloudTaskMessage prepareEqFlushCloudTaskMessage(
      UacQidLink uacQidLink, UUID transactionId) {
    CloudTaskMessage cloudTaskMessage = new CloudTaskMessage();
    cloudTaskMessage.setCloudTaskType(EQ_FLUSH);

    EqFlushTaskPayload eqFlushTaskPayload = new EqFlushTaskPayload();
    eqFlushTaskPayload.setQid(uacQidLink.getQid());
    eqFlushTaskPayload.setTransactionId(transactionId);
    cloudTaskMessage.setPayload(eqFlushTaskPayload);

    return cloudTaskMessage;
  }
}
