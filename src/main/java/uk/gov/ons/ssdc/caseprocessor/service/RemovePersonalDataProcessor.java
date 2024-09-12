package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.common.model.entity.ActionRule;
import uk.gov.ons.ssdc.common.model.entity.Case;

@Component
public class RemovePersonalDataProcessor {
  private final MessageSender messageSender;
  private final EventRepository eventRepository;
  private final UacQidLinkRepository uacQidLinkRepository;
  private final CaseRepository caseRepository;
  private final CaseToProcessRepository caseToProcessRepository;

  @Value("${queueconfig.rh-remove-personal-data-topic}")
  private String removePersonalDataTopic;

  public RemovePersonalDataProcessor(
      MessageSender messageSender,
      EventRepository eventRepository,
      UacQidLinkRepository uacQidLinkRepository,
      CaseRepository caseRepository,
      CaseToProcessRepository caseToProcessRepository) {
    this.messageSender = messageSender;
    this.eventRepository = eventRepository;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.caseRepository = caseRepository;
    this.caseToProcessRepository = caseToProcessRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(Case caze, ActionRule actionRule) {
    UUID caseId = caze.getId();

    deletePersonalDataFromDB(caseId);

    CaseUpdateDTO caseUpdateDTO = new CaseUpdateDTO();
    caseUpdateDTO.setCaseId(caze.getId());

    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO(
            removePersonalDataTopic, actionRule.getId(), actionRule.getCreatedBy());

    EventDTO event = new EventDTO();
    PayloadDTO payload = new PayloadDTO();
    event.setHeader(eventHeader);
    event.setPayload(payload);
    payload.setCaseUpdate(caseUpdateDTO);

    messageSender.sendMessage(removePersonalDataTopic, event);
  }

  private void deletePersonalDataFromDB(UUID caseId) {
    eventRepository.deleteByCazeId(caseId);
    uacQidLinkRepository.deleteByCazeId(caseId);
    caseToProcessRepository.deleteByCazeId(caseId);
    caseRepository.deleteById(caseId);
  }
}
