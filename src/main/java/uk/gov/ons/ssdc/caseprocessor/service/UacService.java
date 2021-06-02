package uk.gov.ons.ssdc.caseprocessor.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

import java.util.Optional;

@Service
public class UacService {
  private static final String UAC_UPDATE_ROUTING_KEY = "event.uac.update";
  private static final int CCS_INTERVIEWER_HOUSEHOLD_QUESTIONNAIRE_FOR_ENGLAND_AND_WALES = 71;

  private final UacQidLinkRepository uacQidLinkRepository;
  private final RabbitTemplate rabbitTemplate;


  @Value("${queueconfig.case-event-exchange}")
  private String outboundExchange;

  public UacService(
      UacQidLinkRepository uacQidLinkRepository,
      RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }



  public void saveAndEmitUacUpdatedEvent(UacQidLink uacQidLink) {
//    uacQidLinkRepository.save(uacQidLink);
//
//    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.UAC_UPDATED);
//
//    UacDTO uac = new UacDTO();
//    uac.setQuestionnaireId(uacQidLink.getQid());
//    uac.setUacHash(Sha256Helper.hash(uacQidLink.getUac()));
//    uac.setUac(uacQidLink.getUac());
//    uac.setActive(uacQidLink.isActive());
//    // It's perfectly possible to derive the Form Type from the supplied data, so RM should not be
//    // forced to incorporate CENSUS business logic. It's for the CENSUS team to put business logic
//    // wherever it's needed, which quite clearly is NOT here. TODO: Put the business logic elsewhere
//    String formType = mapQuestionnaireTypeToFormType(uacQidLink.getQid());
//    if (CONT_FORM_TYPE.equals(formType)) {
//      // We want to send out null form type rather than "Cont" for continuation questionnaires
//      // since they are not a valid form type in EQ/RH
//      formType = null;
//    }
//    uac.setFormType(formType);
//
//    Case caze = uacQidLink.getCaze();
//    if (caze != null) {
//      uac.setCaseId(caze.getCaseId());
//      uac.setCaseType(caze.getCaseType());
//      uac.setCollectionExerciseId(caze.getCollectionExerciseId());
//      uac.setRegion(caze.getRegion());
//    }
//
//    PayloadDTO payloadDTO = new PayloadDTO();
//    payloadDTO.setUac(uac);
//    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
//    responseManagementEvent.setEvent(eventDTO);
//    responseManagementEvent.setPayload(payloadDTO);
//
//    rabbitTemplate.convertAndSend(
//        outboundExchange, UAC_UPDATE_ROUTING_KEY, responseManagementEvent);
//
//    return payloadDTO;
  }

  public UacQidLink findByQid(String questionnaireId) {
    Optional<UacQidLink> uacQidLinkOpt = uacQidLinkRepository.findByQid(questionnaireId);

    if (uacQidLinkOpt.isEmpty()) {
      throw new RuntimeException(
          String.format("Questionnaire Id '%s' not found!", questionnaireId));
    }

    return uacQidLinkOpt.get();
  }
}
