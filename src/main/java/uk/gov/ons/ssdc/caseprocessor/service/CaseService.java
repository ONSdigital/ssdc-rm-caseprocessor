package uk.gov.ons.ssdc.caseprocessor.service;

import static com.google.cloud.spring.pubsub.support.PubSubTopicUtils.toProjectTopicName;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.CaseUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RefusalTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;

@Service
public class CaseService {
  private final CaseRepository caseRepository;
  private final MessageSender messageSender;

  @Value("${queueconfig.case-update-topic}")
  private String caseUpdateTopic;

  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  public CaseService(CaseRepository caseRepository, MessageSender messageSender) {
    this.caseRepository = caseRepository;
    this.messageSender = messageSender;
  }

  public void saveCaseAndEmitCaseUpdate(Case caze) {
    saveCase(caze);
    emitCaseUpdate(caze);
  }

  public void saveCase(Case caze) {
    caseRepository.saveAndFlush(caze);
  }

  public void emitCaseUpdate(Case caze) {
    EventHeaderDTO eventHeader = EventHelper.createEventDTO(caseUpdateTopic);

    EventDTO event = prepareCaseEvent(caze, eventHeader);

    String topic = toProjectTopicName(caseUpdateTopic, sharedPubsubProject).toString();
    messageSender.sendMessage(topic, event);
  }

  private EventDTO prepareCaseEvent(Case caze, EventHeaderDTO eventHeader) {
    PayloadDTO payloadDTO = new PayloadDTO();
    CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
    caseUpdate.setCaseId(caze.getId());
    caseUpdate.setSample(caze.getSample());
    caseUpdate.setReceiptReceived(caze.isReceiptReceived());
    caseUpdate.setInvalid(caze.isInvalid());
    caseUpdate.setSurveyLaunched(caze.isSurveyLaunched());
    if (caze.getRefusalReceived() != null) {
      caseUpdate.setRefusalReceived(RefusalTypeDTO.valueOf(caze.getRefusalReceived().name()));
    } else {
      caseUpdate.setRefusalReceived(null);
    }
    payloadDTO.setCaseUpdate(caseUpdate);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);
    return event;
  }

  public Case getCaseByCaseId(UUID caseId) {
    Optional<Case> cazeResult = caseRepository.findById(caseId);

    if (cazeResult.isEmpty()) {
      throw new RuntimeException(String.format("Case ID '%s' not present", caseId));
    }
    return cazeResult.get();
  }
}
