package uk.gov.ons.ssdc.caseprocessor.service;

import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.DEACTIVATE_UAC_ROUTING_KEY;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.DeactivateUacDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventTypeDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ResponseManagementEvent;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;

@Component
public class DeactivateUacProcessor {
  private final RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.case-event-exchange}")
  private String outboundExchange;

  public DeactivateUacProcessor(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void process(Case caze) {
    List<UacQidLink> uacQidLinks = caze.getUacQidLinks();

    for (UacQidLink uacQidLink : uacQidLinks) {
      if (uacQidLink.isActive()) {
        ResponseManagementEvent responseManagementEvent = prepareDeactivateUacEvent(uacQidLink);
        rabbitTemplate.convertAndSend(
            outboundExchange, DEACTIVATE_UAC_ROUTING_KEY, responseManagementEvent);
      }
    }
  }

  private ResponseManagementEvent prepareDeactivateUacEvent(UacQidLink uacQidLink) {
    EventDTO eventDTO = new EventDTO();
    eventDTO.setTransactionId(UUID.randomUUID());
    eventDTO.setType(EventTypeDTO.DEACTIVATE_UAC);
    eventDTO.setChannel("RM");
    eventDTO.setSource("CASE_PROCESSOR");
    eventDTO.setDateTime(OffsetDateTime.now());

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
