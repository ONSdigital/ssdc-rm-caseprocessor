package uk.gov.ons.ssdc.caseprocessor.service;

import java.nio.charset.StandardCharsets;
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
import uk.gov.ons.ssdc.caseprocessor.utils.HashHelper;

@Service
public class UacService {
  private final UacQidLinkRepository uacQidLinkRepository;
  private final MessageSender messageSender;

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  public UacService(UacQidLinkRepository uacQidLinkRepository, MessageSender messageSender) {
    this.messageSender = messageSender;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public UacQidLink saveAndEmitUacUpdateEvent(UacQidLink uacQidLink) {
    UacQidLink savedUacQidLink = uacQidLinkRepository.save(uacQidLink);

    EventHeaderDTO eventHeader = EventHelper.createEventDTO(uacUpdateTopic);

    UacUpdateDTO uac = new UacUpdateDTO();
    uac.setQid(savedUacQidLink.getQid());

    byte[] encodedUacHash =
        HashHelper.getSHA256(savedUacQidLink.getUac().getBytes(StandardCharsets.UTF_8));

    uac.setUacHash(HashHelper.bytesToHexString(encodedUacHash));
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

    messageSender.sendMessage(uacUpdateTopic, event);

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
