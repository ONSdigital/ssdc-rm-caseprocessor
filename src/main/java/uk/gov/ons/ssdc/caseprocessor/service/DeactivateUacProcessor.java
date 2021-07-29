package uk.gov.ons.ssdc.caseprocessor.service;

import static uk.gov.ons.ssdc.caseprocessor.utils.EventHelper.createEventDTO;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DeactivateUacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;

@Component
public class DeactivateUacProcessor {
  private final MessageSender messageSender;

  @Value("${queueconfig.deactivate-uac-topic}")
  private String deactivateUacTopic;

  public DeactivateUacProcessor(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  public void process(Case caze) {
    List<UacQidLink> uacQidLinks = caze.getUacQidLinks();

    for (UacQidLink uacQidLink : uacQidLinks) {
      if (uacQidLink.isActive()) {
        ResponseManagementEvent responseManagementEvent = prepareDeactivateUacEvent(uacQidLink);
        messageSender.sendMessage(deactivateUacTopic, responseManagementEvent);
      }
    }
  }

  private ResponseManagementEvent prepareDeactivateUacEvent(UacQidLink uacQidLink) {
    EventDTO eventDTO = createEventDTO(EventTypeDTO.DEACTIVATE_UAC);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(eventDTO);

    PayloadDTO payloadDTO = new PayloadDTO();
    DeactivateUacDTO deactivateUacDTO = new DeactivateUacDTO();
    deactivateUacDTO.setQid(uacQidLink.getQid());
    payloadDTO.setDeactivateUac(deactivateUacDTO);
    responseManagementEvent.setPayload(payloadDTO);
    return responseManagementEvent;
  }
}
