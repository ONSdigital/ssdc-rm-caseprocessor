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
import uk.gov.ons.ssdc.common.model.entity.Case;
import uk.gov.ons.ssdc.common.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.common.model.entity.PrintTemplate;

@Component
public class RasRmProcessor {
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

  public RasRmProcessor(
      RasRmPartyServiceClient rasRmPartyServiceClient,
      MessageSender messageSender,
      PrintProcessor printProcessor) {
    this.rasRmPartyServiceClient = rasRmPartyServiceClient;
    this.messageSender = messageSender;
    this.printProcessor = printProcessor;
  }

  public void process(CaseToProcess caseToProcess) {
    boolean activeEnrolment = sendCaseToRasRmAndGetActiveEnrolmentFlag(caseToProcess);

    // If we don't have an active enrolment for the business, then we should send a letter
    if (!activeEnrolment) {
      printMpsRow(caseToProcess);
    }
  }

  private boolean sendCaseToRasRmAndGetActiveEnrolmentFlag(CaseToProcess caseToProcess) {
    Map<String, String> metadataHack;
    try {
      metadataHack = (Map<String, String>) caseToProcess.getActionRule().getUacMetadata();
    } catch (ClassCastException castException) {
      throw new RuntimeException("Cannot execute action rule if metadata not in expected format");
    }

    String rasRmCollectionExerciseIdString = metadataHack.get("rasRmCollectionExerciseId");

    if (!StringUtils.hasText(rasRmCollectionExerciseIdString)) {
      throw new RuntimeException("rasRmCollectionExerciseId is mandatory in metadata");
    }

    UUID rasRmCollectionExerciseId;

    try {
      rasRmCollectionExerciseId = UUID.fromString(rasRmCollectionExerciseIdString);
    } catch (Exception e) {
      throw new RuntimeException("rasRmCollectionExerciseId must be a valid UUID");
    }

    String rasRmCollectionInstrumentIdString = metadataHack.get("rasRmCollectionInstrumentId");

    if (!StringUtils.hasText(rasRmCollectionInstrumentIdString)) {
      throw new RuntimeException("rasRmCollectionInstrumentId is mandatory in metadata");
    }

    UUID rasRmCollectionInstrumentId;

    try {
      rasRmCollectionInstrumentId = UUID.fromString(rasRmCollectionInstrumentIdString);
    } catch (Exception e) {
      throw new RuntimeException("rasRmCollectionInstrumentId must be a valid UUID");
    }

    Case caze = caseToProcess.getCaze();

    for (String mandatoryColumn : MANDATORY_SAMPLE_COLUMNS) {
      if (!StringUtils.hasText(caze.getSample().get(mandatoryColumn))) {
        throw new RuntimeException(
            "Cannot share case with RAS-RM which does not have: " + mandatoryColumn);
      }
    }

    String ruRef = caze.getSample().get("ruref");

    Map<String, String> attributesExcludingPointlessIntegers =
        getAttributesExcludingPointlessIntegers(caze);

    RasRmPartyResponseDTO party =
        rasRmPartyServiceClient.createParty(
            ruRef, caseToProcess.getBatchId(), attributesExcludingPointlessIntegers);

    boolean activeEnrolment =
        Arrays.stream(party.getAssociations())
            .anyMatch(enrolment -> enrolment.getBusinessRespondentStatus().equals("ACTIVE"));

    RasRmCaseNotification caseNotification = new RasRmCaseNotification();
    caseNotification.setId(caseToProcess.getBatchId());
    caseNotification.setActiveEnrolment(activeEnrolment);
    caseNotification.setSampleUnitRef(ruRef);
    caseNotification.setSampleUnitType("B"); // B = business. No need for any other value
    caseNotification.setPartyId(party.getId());
    caseNotification.setCollectionInstrumentId(rasRmCollectionInstrumentId);
    caseNotification.setCollectionExerciseId(rasRmCollectionExerciseId);

    String topic = toProjectTopicName(rasRmCaseNotificationTopic, rasRmPubsubProject).toString();

    messageSender.sendMessage(topic, caseNotification);

    return activeEnrolment;
  }

  private Map<String, String> getAttributesExcludingPointlessIntegers(Case caze) {
    // TODO: Maybe do something different with these... if they turn out not to be pointless
    Map<String, String> attributesExcludingPointlessIntegers = new HashMap<>();
    for (String key : caze.getSample().keySet()) {
      if (!POINTLESS_INTEGER_COLUMNS.contains(key)) {
        attributesExcludingPointlessIntegers.put(key, caze.getSample().get(key));
      }
    }
    return attributesExcludingPointlessIntegers;
  }

  private void printMpsRow(CaseToProcess caseToProcess) {
    PrintTemplate printTemplate = caseToProcess.getActionRule().getPrintTemplate();
    printProcessor.processPrintRow(
        printTemplate.getTemplate(),
        caseToProcess.getCaze(),
        caseToProcess.getBatchId(),
        caseToProcess.getBatchQuantity(),
        printTemplate.getPackCode(),
        printTemplate.getPrintSupplier(),
        caseToProcess.getActionRule().getId(),
        null,
        null);
  }
}
