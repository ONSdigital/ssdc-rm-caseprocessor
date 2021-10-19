package uk.gov.ons.ssdc.caseprocessor.service;

import static com.google.cloud.spring.pubsub.support.PubSubTopicUtils.toProjectTopicName;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.ons.ssdc.caseprocessor.client.RasRmPartyServiceClient;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PartyResponseDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.SampleUnitParentDTO;
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CaseToProcess;

@Component
public class RasRmProcessor {
  private static final Logger log = LoggerFactory.getLogger(RasRmProcessor.class);
  private static final String[] MANDATORY_SAMPLE_COLUMNS = {"ruref", "runame1"};

  // TODO: Maybe do something different with these... if they turn out not to be pointless
  private static final Set<String> POINTLESS_INTEGER_COLUMNS =
      Set.of("froempment", "frotover", "cell_no");

  private final RasRmPartyServiceClient rasRmPartyServiceClient;
  private final MessageSender messageSender;

  @Value("${queueconfig.ras-rm-case-notification-topic}")
  private String rasRmCaseNotificationTopic;

  @Value("${queueconfig.ras-rm-pubsub-project")
  private String rasRmPubsubProject;

  public RasRmProcessor(
      RasRmPartyServiceClient rasRmPartyServiceClient, MessageSender messageSender) {
    this.rasRmPartyServiceClient = rasRmPartyServiceClient;
    this.messageSender = messageSender;
  }

  public void process(CaseToProcess caseToProcess) {
    Map<String, String> metadataHack;
    try {
      metadataHack = (Map<String, String>) caseToProcess.getActionRule().getUacMetadata();
    } catch (ClassCastException castException) {
      log.warn("Cannot execute action rule if metadata not in expected format");
      return;
    }

    String rasRmCollectionExerciseIdString = metadataHack.get("rasRmCollectionExerciseId");

    if (!StringUtils.hasText(rasRmCollectionExerciseIdString)) {
      log.warn("rasRmCollectionExerciseId is mandatory in metadata");
      return;
    }

    UUID rasRmCollectionExerciseId;

    try {
      rasRmCollectionExerciseId = UUID.fromString(rasRmCollectionExerciseIdString);
    } catch (Exception e) {
      log.warn("rasRmCollectionExerciseId must be a valid UUID");
      return;
    }

    String rasRmCollectionInstrumentIdString = metadataHack.get("rasRmCollectionInstrumentId");

    if (!StringUtils.hasText(rasRmCollectionInstrumentIdString)) {
      log.warn("rasRmCollectionInstrumentId is mandatory in metadata");
      return;
    }

    UUID rasRmCollectionInstrumentId;

    try {
      rasRmCollectionInstrumentId = UUID.fromString(rasRmCollectionInstrumentIdString);
    } catch (Exception e) {
      log.warn("rasRmCollectionInstrumentId must be a valid UUID");
      return;
    }

    Case caze = caseToProcess.getCaze();

    for (String mandatoryColumn : MANDATORY_SAMPLE_COLUMNS) {
      if (!StringUtils.hasText(caze.getSample().get(mandatoryColumn))) {
        log.warn("Cannot share case with RAS-RM which does not have: " + mandatoryColumn);
        return;
      }
    }

    String ruRef = caze.getSample().get("ruref");

    // TODO: Maybe do something different with these... if they turn out not to be pointless
    Map<String, String> attributesExcludingPointlessIntegers = new HashMap<>();
    for (String key : caze.getSample().keySet()) {
      if (!POINTLESS_INTEGER_COLUMNS.contains(key)) {
        attributesExcludingPointlessIntegers.put(key, caze.getSample().get(key));
      }
    }

    // TODO: this can fail if RU ref already in Party Service... in which case we need to get ID
    PartyResponseDTO party =
        rasRmPartyServiceClient.createParty(
            ruRef, caseToProcess.getBatchId(), attributesExcludingPointlessIntegers);

    log.info("Created RAS-RM party with ID: " + party.getId().toString());

    SampleUnitParentDTO sampleUnitParentDTO = new SampleUnitParentDTO();
    sampleUnitParentDTO.setId(caseToProcess.getBatchId());
    sampleUnitParentDTO.setActiveEnrolment(false); // TODO: check if business has enrolment
    sampleUnitParentDTO.setSampleUnitRef(ruRef);
    sampleUnitParentDTO.setSampleUnitType(
        "B"); // Hard-coded to be B = business. No need for any other value
    sampleUnitParentDTO.setPartyId(party.getId());
    sampleUnitParentDTO.setCollectionInstrumentId(rasRmCollectionExerciseId);
    sampleUnitParentDTO.setCollectionExerciseId(rasRmCollectionInstrumentId);

    String topic = toProjectTopicName(rasRmCaseNotificationTopic, rasRmPubsubProject).toString();

    messageSender.sendMessage(topic, sampleUnitParentDTO);
  }
}
