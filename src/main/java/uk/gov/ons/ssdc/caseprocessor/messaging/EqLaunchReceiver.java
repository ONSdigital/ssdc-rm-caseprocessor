package uk.gov.ons.ssdc.caseprocessor.messaging;

import static uk.gov.ons.ssdc.caseprocessor.utils.JsonHelper.convertJsonBytesToEvent;
import static uk.gov.ons.ssdc.caseprocessor.utils.MsgDateHelper.getMsgTimeStamp;

import java.time.OffsetDateTime;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.service.CaseService;
import uk.gov.ons.ssdc.caseprocessor.service.UacService;
import uk.gov.ons.ssdc.common.model.entity.EventType;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@MessageEndpoint
public class EqLaunchReceiver {
  private final UacService uacService;
  private final CaseService caseService;
  private final EventLogger eventLogger;

  public EqLaunchReceiver(UacService uacService, CaseService caseService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  @Transactional
  @ServiceActivator(inputChannel = "eqLaunchInputChannel", adviceChain = "retryAdvice")
  public void receiveMessage(Message<byte[]> message) {
    EventDTO event = convertJsonBytesToEvent(message.getPayload());

    OffsetDateTime messageTimestamp = getMsgTimeStamp(message);

    UacQidLink uacQidLink = uacService.findByQid(event.getPayload().getEqLaunch().getQid());

    eventLogger.logUacQidEvent(
        uacQidLink,
        event.getHeader().getDateTime(),
        "EQ launched",
        EventType.EQ_LAUNCH,
        event.getHeader(),
        event.getPayload().getEqLaunch(),
        messageTimestamp);

    uacQidLink.getCaze().setEqLaunched(true);
    caseService.saveCaseAndEmitCaseUpdate(
        uacQidLink.getCaze(),
        event.getHeader().getCorrelationId(),
        event.getHeader().getOriginatingUser());
  }
}
