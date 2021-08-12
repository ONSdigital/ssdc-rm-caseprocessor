package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacDTO;
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

  public UacService(UacQidLinkRepository uacQidLinkRepository, MessageSender messageSender) {
    this.messageSender = messageSender;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public UacQidLink saveAndEmitUacUpdatedEvent(UacQidLink uacQidLink) {
    UacQidLink savedUacQidLink = uacQidLinkRepository.save(uacQidLink);

    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.UAC_UPDATED);

    UacDTO uac = new UacDTO();
    uac.setQuestionnaireId(savedUacQidLink.getQid());
    uac.setUac(savedUacQidLink.getUac());
    uac.setActive(savedUacQidLink.isActive());

    Case caze = savedUacQidLink.getCaze();
    if (caze != null) {
      uac.setCaseId(caze.getId());
      uac.setCollectionExerciseId(caze.getCollectionExercise().getId());
    }

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUac(uac);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(eventDTO);
    responseManagementEvent.setPayload(payloadDTO);

    messageSender.sendMessage(uacUpdateTopic, responseManagementEvent);

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
