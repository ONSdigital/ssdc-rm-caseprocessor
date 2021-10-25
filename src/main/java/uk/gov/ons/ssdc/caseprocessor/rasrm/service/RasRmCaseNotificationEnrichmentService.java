package uk.gov.ons.ssdc.caseprocessor.rasrm.service;

import static com.google.cloud.spring.pubsub.support.PubSubTopicUtils.toProjectTopicName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.rasrm.client.RasRmPartyServiceClient;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmCaseNotification;
import uk.gov.ons.ssdc.caseprocessor.rasrm.model.dto.RasRmPartyResponseDTO;

@Component
public class RasRmCaseNotificationEnrichmentService {
  private static final Set<String> MANDATORY_COLLEX_METADATA =
      Set.of("rasRmSampleSummaryId", "rasRmCollectionExerciseId", "rasRmCollectionInstrumentId");
  private static final String[] MANDATORY_SAMPLE_COLUMNS = {"ruref", "runame1"};
  private static final Set<String> INTEGER_PARTY_ATTRIBUTES =
      Set.of("froempment", "frotover", "cell_no");

  private final RasRmPartyServiceClient rasRmPartyServiceClient;
  private final MessageSender messageSender;

  @Value("${queueconfig.ras-rm-case-notification-topic}")
  private String rasRmCaseNotificationTopic;

  @Value("${queueconfig.ras-rm-pubsub-project}")
  private String rasRmPubsubProject;

  public RasRmCaseNotificationEnrichmentService(
      RasRmPartyServiceClient rasRmPartyServiceClient, MessageSender messageSender) {
    this.rasRmPartyServiceClient = rasRmPartyServiceClient;
    this.messageSender = messageSender;
  }

  public Map<String, String> notifyRasRmAndEnrichSample(
      Map<String, String> sample, Object metadataObject) {
    if (metadataObject == null) {
      throw new RuntimeException(
          "Unexpected null metadata. Metadata is required for RAS-RM business.");
    }

    if (!(metadataObject instanceof Map)) {
      throw new RuntimeException(
          "Unexpected metadata type. Wanted Map but got "
              + metadataObject.getClass().getSimpleName());
    }

    Map metadata = (Map) metadataObject;

    if (!metadata.keySet().containsAll(MANDATORY_COLLEX_METADATA)) {
      throw new RuntimeException("Metadata does not contain mandatory values");
    }

    UUID rasRmSampleSummaryId = UUID.fromString((String) metadata.get("rasRmSampleSummaryId"));
    UUID rasRmCollectionExerciseId =
        UUID.fromString((String) metadata.get("rasRmCollectionExerciseId"));
    UUID rasRmCollectionInstrumentId =
        UUID.fromString((String) metadata.get("rasRmCollectionInstrumentId"));

    for (String mandatoryColumn : MANDATORY_SAMPLE_COLUMNS) {
      if (!StringUtils.hasText(sample.get(mandatoryColumn))) {
        throw new RuntimeException(
            "Cannot notify RAS-RM of business case which does not have column: " + mandatoryColumn);
      }
    }

    String ruRef = sample.get("ruref");

    Map<String, Object> partyAttributes = covertIntegerPartyAttributes(sample);

    RasRmPartyResponseDTO party =
        rasRmPartyServiceClient.createParty(ruRef, rasRmSampleSummaryId, partyAttributes);

    boolean activeEnrolment =
        Arrays.stream(party.getAssociations())
            .anyMatch(enrolment -> enrolment.getBusinessRespondentStatus().equals("ACTIVE"));

    RasRmCaseNotification caseNotification = new RasRmCaseNotification();
    caseNotification.setId(rasRmSampleSummaryId);
    caseNotification.setActiveEnrolment(activeEnrolment);
    caseNotification.setSampleUnitRef(ruRef);
    caseNotification.setSampleUnitType("B"); // B = business. No need for any other value
    caseNotification.setPartyId(party.getId());
    caseNotification.setCollectionInstrumentId(rasRmCollectionInstrumentId);
    caseNotification.setCollectionExerciseId(rasRmCollectionExerciseId);

    String topic = toProjectTopicName(rasRmCaseNotificationTopic, rasRmPubsubProject).toString();

    messageSender.sendMessage(topic, caseNotification);

    Map<String, String> enrichedSample = new HashMap<>(sample);
    enrichedSample.put("activeEnrolment", Boolean.toString(activeEnrolment));
    enrichedSample.put("partyId", party.getId().toString());
    return enrichedSample;
  }

  private Map<String, Object> covertIntegerPartyAttributes(Map<String, String> sample) {
    Map<String, Object> partyAttributes = new HashMap<>();
    for (String key : sample.keySet()) {
      if (INTEGER_PARTY_ATTRIBUTES.contains(key)) {
        partyAttributes.put(key, Integer.valueOf(sample.get(key)));
      } else {
        partyAttributes.put(key, sample.get(key));
      }
    }

    return partyAttributes;
  }
}
