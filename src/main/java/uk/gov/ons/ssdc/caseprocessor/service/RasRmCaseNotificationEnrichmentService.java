package uk.gov.ons.ssdc.caseprocessor.service;

import static com.google.cloud.spring.pubsub.support.PubSubTopicUtils.toProjectTopicName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.ons.ssdc.caseprocessor.client.RasRmPartyServiceClient;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RasRmCaseNotification;
import uk.gov.ons.ssdc.caseprocessor.model.dto.RasRmPartyResponseDTO;

@Component
public class RasRmCaseNotificationEnrichmentService {
  private static final String[] MANDATORY_SAMPLE_COLUMNS = {"ruref", "runame1"};

  // TODO: Maybe do something different with these... if they turn out not to be pointless
  private static final Set<String> POINTLESS_INTEGER_COLUMNS =
      Set.of("froempment", "frotover", "cell_no");

  private final RasRmPartyServiceClient rasRmPartyServiceClient;
  private final MessageSender messageSender;
  private final PrintProcessor printProcessor;

  @Value("${queueconfig.ras-rm-case-notification-topic}")
  private String rasRmCaseNotificationTopic;

  @Value("${queueconfig.ras-rm-pubsub-project}")
  private String rasRmPubsubProject;

  public RasRmCaseNotificationEnrichmentService(
      RasRmPartyServiceClient rasRmPartyServiceClient,
      MessageSender messageSender,
      PrintProcessor printProcessor) {
    this.rasRmPartyServiceClient = rasRmPartyServiceClient;
    this.messageSender = messageSender;
    this.printProcessor = printProcessor;
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

    Map<String, String> attributesExcludingPointlessIntegers =
        getAttributesExcludingPointlessIntegers(sample);

    RasRmPartyResponseDTO party =
        rasRmPartyServiceClient.createParty(
            ruRef, rasRmSampleSummaryId, attributesExcludingPointlessIntegers);

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
    return enrichedSample;
  }

  private Map<String, String> getAttributesExcludingPointlessIntegers(Map<String, String> sample) {
    // TODO: Maybe do something different with these... if they turn out not to be pointless
    Map<String, String> attributesExcludingPointlessIntegers = new HashMap<>();
    for (String key : sample.keySet()) {
      if (!POINTLESS_INTEGER_COLUMNS.contains(key)) {
        attributesExcludingPointlessIntegers.put(key, sample.get(key));
      }
    }
    return attributesExcludingPointlessIntegers;
  }
}
