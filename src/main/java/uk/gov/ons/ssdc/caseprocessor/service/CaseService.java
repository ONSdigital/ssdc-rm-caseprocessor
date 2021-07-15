package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CollectionCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;

@Service
public class CaseService {
  private final CaseRepository caseRepository;
  private final RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.case-event-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.case-update-routing-key}")
  private String caseUpdateRoutingKey;

  public CaseService(CaseRepository caseRepository, RabbitTemplate rabbitTemplate) {
    this.caseRepository = caseRepository;
    this.rabbitTemplate = rabbitTemplate;
  }

  public void saveCaseAndEmitCaseUpdatedEvent(Case caze) {
    caseRepository.saveAndFlush(caze);

    EventDTO eventDTO = new EventDTO();
    eventDTO.setType(EventTypeDTO.CASE_UPDATED);
    eventDTO.setChannel("RM");
    ResponseManagementEvent responseManagementEvent = prepareCaseEvent(caze, eventDTO);
    rabbitTemplate.convertAndSend(outboundExchange, caseUpdateRoutingKey, responseManagementEvent);
  }

  public void saveCase(Case caze) {
    caseRepository.saveAndFlush(caze);
  }

  public void emitCaseCreatedEvent(Case caze) {
    EventDTO eventDTO = new EventDTO();
    eventDTO.setType(EventTypeDTO.CASE_CREATED);
    eventDTO.setChannel("RM");
    ResponseManagementEvent responseManagementEvent = prepareCaseEvent(caze, eventDTO);
    rabbitTemplate.convertAndSend(outboundExchange, caseUpdateRoutingKey, responseManagementEvent);
  }

  private ResponseManagementEvent prepareCaseEvent(Case caze, EventDTO eventDTO) {
    PayloadDTO payloadDTO = new PayloadDTO();
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setCaseId(caze.getId());
    collectionCase.setSample(caze.getSample());
    collectionCase.setReceiptReceived(caze.isReceiptReceived());
    collectionCase.setInvalidAddress(caze.isAddressInvalid());
    collectionCase.setSurveyLaunched(caze.isSurveyLaunched());
    if (caze.getRefusalReceived() != null) {
      collectionCase.setRefusalReceived(RefusalTypeDTO.valueOf(caze.getRefusalReceived().name()));
    } else {
      collectionCase.setRefusalReceived(null);
    }
    payloadDTO.setCollectionCase(collectionCase);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(eventDTO);
    responseManagementEvent.setPayload(payloadDTO);
    return responseManagementEvent;
  }

  public Case getCaseByCaseId(UUID caseId) {
    Optional<Case> cazeResult = caseRepository.findById(caseId);

    if (cazeResult.isEmpty()) {
      throw new RuntimeException(String.format("Case ID '%s' not present", caseId));
    }
    return cazeResult.get();
  }
}
