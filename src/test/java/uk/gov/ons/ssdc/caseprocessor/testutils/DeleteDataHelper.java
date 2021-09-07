package uk.gov.ons.ssdc.caseprocessor.testutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ActionRuleSurveyPrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ClusterLeaderRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CollectionExerciseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentNextTriggerRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentSurveyPrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.SurveyRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@Component
@ActiveProfiles("test")
public class DeleteDataHelper {
  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private SurveyRepository surveyRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private FulfilmentNextTriggerRepository fulfilmentNextTriggerRepository;
  @Autowired private FulfilmentToProcessRepository fulfilmentToProcessRepository;
  @Autowired private PrintTemplateRepository printTemplateRepository;

  @Autowired
  private FulfilmentSurveyPrintTemplateRepository fulfilmentSurveyPrintTemplateRepository;

  @Autowired
  private ActionRuleSurveyPrintTemplateRepository actionRuleSurveyPrintTemplateRepository;

  @Autowired private ActionRuleRepository actionRuleRepository;
  @Autowired private CaseToProcessRepository caseToProcessRepository;
  @Autowired private ClusterLeaderRepository clusterLeaderRepository;

  @Autowired private PrintFileRowRepository printFileRowRepository;

  @Transactional
  public void deleteAllData() {
    actionRuleRepository.deleteAllInBatch();
    fulfilmentNextTriggerRepository.deleteAllInBatch();
    caseToProcessRepository.deleteAllInBatch();
    fulfilmentToProcessRepository.deleteAllInBatch();
    eventRepository.deleteAllInBatch();
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    collectionExerciseRepository.deleteAllInBatch();
    fulfilmentSurveyPrintTemplateRepository.deleteAllInBatch();
    actionRuleSurveyPrintTemplateRepository.deleteAllInBatch();
    printTemplateRepository.deleteAllInBatch();
    surveyRepository.deleteAllInBatch();
    clusterLeaderRepository.deleteAllInBatch();
    printFileRowRepository.deleteAllInBatch();
  }
}
