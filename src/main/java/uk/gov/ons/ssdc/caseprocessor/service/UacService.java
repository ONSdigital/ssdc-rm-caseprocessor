package uk.gov.ons.ssdc.caseprocessor.service;

import static org.springframework.cloud.gcp.pubsub.support.PubSubTopicUtils.toProjectTopicName;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;

@Service
public class UacService {
  private final UacQidLinkRepository uacQidLinkRepository;
  private final MessageSender messageSender;

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  public UacService(UacQidLinkRepository uacQidLinkRepository, MessageSender messageSender) {
    this.messageSender = messageSender;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public UacQidLink saveAndEmitUacUpdateEvent(UacQidLink uacQidLink) {
    UacQidLink savedUacQidLink = uacQidLinkRepository.save(uacQidLink);

    EventHeaderDTO eventHeader = EventHelper.createEventDTO(uacUpdateTopic);

    UacUpdateDTO uac = new UacUpdateDTO();
    uac.setQid(savedUacQidLink.getQid());
    uac.setUac(savedUacQidLink.getUac());
    uac.setActive(savedUacQidLink.isActive());

    Case caze = savedUacQidLink.getCaze();
    if (caze != null) {
      uac.setCaseId(caze.getId());
    }

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUacUpdate(uac);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    String topic = toProjectTopicName(uacUpdateTopic, sharedPubsubProject).toString();
    messageSender.sendMessage(topic, event);

    return savedUacQidLink;
  }

  public UacQidLink findByQid(String questionnaireId) {
    Optional<UacQidLink> uacQidLinkOpt = uacQidLinkRepository.findByQid(questionnaireId);

    if (uacQidLinkOpt.isEmpty()) {
      throw new RuntimeException(
          String.format("Questionnaire Id '%s' not found!", questionnaireId));
    }

    return uacQidLinkOpt.get();
  }

  public boolean existsByQid(String qid) {
    return uacQidLinkRepository.existsByQid(qid);
  }
}
