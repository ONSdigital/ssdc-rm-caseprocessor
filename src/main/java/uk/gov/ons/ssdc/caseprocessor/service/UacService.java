package uk.gov.ons.ssdc.caseprocessor.service;

import static com.google.cloud.spring.pubsub.support.PubSubTopicUtils.toProjectTopicName;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ssdc.caseprocessor.collectioninstrument.CollectionInstrumentHelper;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.EventHeaderDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacUpdateDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.HashHelper;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.UacQidLink;

@Service
public class UacService {
  private final UacQidLinkRepository uacQidLinkRepository;
  private final MessageSender messageSender;
  private final CollectionInstrumentHelper collectionInstrumentHelper;

  @Value("${queueconfig.uac-update-topic}")
  private String uacUpdateTopic;

  @Value("${queueconfig.shared-pubsub-project}")
  private String sharedPubsubProject;

  public UacService(
      UacQidLinkRepository uacQidLinkRepository,
      MessageSender messageSender,
      CollectionInstrumentHelper collectionInstrumentHelper) {
    this.messageSender = messageSender;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.collectionInstrumentHelper = collectionInstrumentHelper;
  }

  public UacQidLink saveAndEmitUacUpdateEvent(
      UacQidLink uacQidLink, UUID correlationId, String originatingUser) {
    UacQidLink savedUacQidLink = uacQidLinkRepository.save(uacQidLink);

    EventHeaderDTO eventHeader =
        EventHelper.createEventDTO(uacUpdateTopic, correlationId, originatingUser);

    UacUpdateDTO uac = new UacUpdateDTO();
    uac.setQid(savedUacQidLink.getQid());
    uac.setUacHash(savedUacQidLink.getUacHash());
    uac.setActive(savedUacQidLink.isActive());
    uac.setReceiptReceived(savedUacQidLink.isReceiptReceived());
    uac.setEqLaunched(savedUacQidLink.isEqLaunched());
    uac.setCollectionInstrumentUrl(savedUacQidLink.getCollectionInstrumentUrl());

    uac.setCaseId(savedUacQidLink.getCaze().getId());
    uac.setCollectionExerciseId(savedUacQidLink.getCaze().getCollectionExercise().getId());
    uac.setSurveyId(savedUacQidLink.getCaze().getCollectionExercise().getSurvey().getId());

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setUacUpdate(uac);
    EventDTO event = new EventDTO();
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    String topic = toProjectTopicName(uacUpdateTopic, sharedPubsubProject).toString();
    messageSender.sendMessage(topic, event);

    return savedUacQidLink;
  }

  public UacQidLink findByQid(String qid) {
    Optional<UacQidLink> uacQidLinkOpt = uacQidLinkRepository.findByQid(qid);

    if (uacQidLinkOpt.isEmpty()) {
      throw new RuntimeException(String.format("qid '%s' not found!", qid));
    }

    return uacQidLinkOpt.get();
  }

  public boolean existsByQid(String qid) {
    return uacQidLinkRepository.existsByQid(qid);
  }

  public void createLinkAndEmitNewUacQid(
      Case caze,
      String uac,
      String qid,
      Object metadata,
      UUID correlationId,
      String originatingUser) {

    // TODO: this way is no good if we want to immediately return the CI URL to an API caller
    String collectionInstrumentUrl =
        collectionInstrumentHelper.getCollectionInstrumentUrl(caze, metadata);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setUac(uac);
    uacQidLink.setUacHash(HashHelper.hash(uac));
    uacQidLink.setQid(qid);
    uacQidLink.setMetadata(metadata);
    uacQidLink.setCaze(caze);
    uacQidLink.setCollectionInstrumentUrl(collectionInstrumentUrl);
    saveAndEmitUacUpdateEvent(uacQidLink, correlationId, originatingUser);
  }
}
