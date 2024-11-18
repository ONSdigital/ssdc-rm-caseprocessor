package uk.gov.ons.ssdc.caseprocessor.service;

import static com.google.cloud.spring.pubsub.support.PubSubTopicUtils.toProjectTopicName;
import static uk.gov.ons.ssdc.caseprocessor.utils.EventHelper.createEventDTO;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DeactivateUacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@Component
public class DeactivateUacProcessor {
  private final MessageSender messageSender;

  @Value("${queueconfig.deactivate-uac-topic}")
  private String deactivateUacTopic;

  @Value("${spring.cloud.gcp.pubsub.project-id}")
  private String pubsubProject;

  public DeactivateUacProcessor(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  public void process(Case caze, UUID correlationId) {
    String topic = toProjectTopicName(deactivateUacTopic, pubsubProject).toString();
    List<UacQidLink> uacQidLinks = caze.getUacQidLinks();

    for (UacQidLink uacQidLink : uacQidLinks) {
      if (uacQidLink.isActive()) {
        EventDTO event = prepareDeactivateUacEvent(uacQidLink, correlationId);
        messageSender.sendMessage(topic, event);
      }
    }
  }

  private EventDTO prepareDeactivateUacEvent(UacQidLink uacQidLink, UUID correlationId) {
    EventHeaderDTO eventHeader = createEventDTO(deactivateUacTopic, correlationId, null);

    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);

    PayloadDTO payloadDTO = new PayloadDTO();
    DeactivateUacDTO deactivateUacDTO = new DeactivateUacDTO();
    deactivateUacDTO.setQid(uacQidLink.getQid());
    payloadDTO.setDeactivateUac(deactivateUacDTO);
    event.setPayload(payloadDTO);
    return event;
  }
}
