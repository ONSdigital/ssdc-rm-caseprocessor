package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CollectionCase;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;

@Service
public class CaseService {
  private final CaseRepository caseRepository;
  private final MessageSender messageSender;

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  public CaseService(CaseRepository caseRepository, MessageSender messageSender) {
    this.caseRepository = caseRepository;
    this.messageSender = messageSender;
  }

  public void saveCaseAndEmitCaseUpdatedEvent(Case caze) {
    caseRepository.saveAndFlush(caze);

    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.CASE_UPDATED);

    ResponseManagementEvent responseManagementEvent = prepareCaseEvent(caze, eventDTO);
    messageSender.sendMessage(caseUpdateTopic, responseManagementEvent);
  }

  public void saveCase(Case caze) {
    caseRepository.saveAndFlush(caze);
  }

  public void emitCaseCreatedEvent(Case caze) {
    EventDTO eventDTO = EventHelper.createEventDTO(EventTypeDTO.CASE_CREATED);

    ResponseManagementEvent responseManagementEvent = prepareCaseEvent(caze, eventDTO);
    messageSender.sendMessage(caseUpdateTopic, responseManagementEvent);
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
