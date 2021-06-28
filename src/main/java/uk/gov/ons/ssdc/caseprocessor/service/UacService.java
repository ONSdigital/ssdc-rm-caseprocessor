package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.Optional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
  private static final String UAC_UPDATE_ROUTING_KEY = "events.rh.uacUpdate";

  private final UacQidLinkRepository uacQidLinkRepository;
  private final RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.case-event-exchange}")
  private String outboundExchange;

  public UacService(UacQidLinkRepository uacQidLinkRepository, RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public void saveAndEmitUacUpdatedEvent(UacQidLink uacQidLink) {
    uacQidLinkRepository.save(uacQidLink);

    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.UAC_UPDATED);
    eventDTO.setChannel("RM");

    UacDTO uac = new UacDTO();
    uac.setQuestionnaireId(uacQidLink.getQid());
    uac.setUac(uacQidLink.getUac());
    uac.setActive(uacQidLink.isActive());

    Case caze = uacQidLink.getCaze();
    if (caze != null) {
      uac.setCaseId(caze.getId());
      uac.setCollectionExerciseId(caze.getCollectionExercise().getId());
    }

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUac(uac);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(eventDTO);
    responseManagementEvent.setPayload(payloadDTO);

    rabbitTemplate.convertAndSend(
        outboundExchange, UAC_UPDATE_ROUTING_KEY, responseManagementEvent);
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
