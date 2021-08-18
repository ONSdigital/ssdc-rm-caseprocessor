package uk.gov.ons.ssdc.caseprocessor.service;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;

@Service
public class UacService {
  private final UacQidLinkRepository uacQidLinkRepository;
  private final MessageSender messageSender;
  private static final Logger log = LoggerFactory.getLogger(UacService.class);
  private static MessageDigest digest;

  public static byte[] digest(byte[] input) {
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      log.error("Could not initialise hashing", e);
      throw new RuntimeException("Could not initialise hashing", e);
    }
    byte[] result = digest.digest(input);
    return result;
  }

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  public UacService(UacQidLinkRepository uacQidLinkRepository, MessageSender messageSender) {
    this.messageSender = messageSender;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public UacQidLink saveAndEmitUacUpdateEvent(UacQidLink uacQidLink) {
    UacQidLink savedUacQidLink = uacQidLinkRepository.save(uacQidLink);

    EventHeaderDTO eventHeader = EventHelper.createEventDTO(uacUpdateTopic);

    UacUpdateDTO uac = new UacUpdateDTO();
    uac.setQid(savedUacQidLink.getQid());

    byte[] encodedUacHash = digest(savedUacQidLink.getUac().getBytes(StandardCharsets.UTF_8));

    uac.setUacHash(bytesToHexString(encodedUacHash));
    uac.setActive(savedUacQidLink.isActive());

    Case caze = savedUacQidLink.getCaze();
    if (caze != null) {
      uac.setCaseId(caze.getId());
    }

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUacUpdate(uac);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    messageSender.sendMessage(uacUpdateTopic, event);

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

  private String bytesToHexString(byte[] hash) {
    StringBuilder hexString = new StringBuilder();
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
