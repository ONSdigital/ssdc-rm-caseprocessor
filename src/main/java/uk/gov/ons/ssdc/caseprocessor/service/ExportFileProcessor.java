package uk.gov.ons.ssdc.caseprocessor.service;

import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.collectioninstrument.CollectionInstrumentSelectionRule;
import uk.gov.ons.ssdc.caseprocessor.collectioninstrument.EvaluationBundle;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.rasrm.service.RasRmCaseIacService;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.common.model.entity.*;

@Component
public class ExportFileProcessor {
  private final UacQidCache uacQidCache;
  private final UacService uacService;
  private final EventLogger eventLogger;
  private final ExportFileRowRepository exportFileRowRepository;
  private final RasRmCaseIacService rasRmCaseIacService;

  private final StringWriter stringWriter = new StringWriter();
  private final CSVWriter csvWriter =
      new CSVWriter(
          stringWriter,
          '|',
          CSVWriter.DEFAULT_QUOTE_CHARACTER,
          CSVWriter.DEFAULT_ESCAPE_CHARACTER,
          "");

  public ExportFileProcessor(
      UacQidCache uacQidCache,
      UacService uacService,
      EventLogger eventLogger,
      ExportFileRowRepository exportFileRowRepository,
      RasRmCaseIacService rasRmCaseIacService) {
    this.uacQidCache = uacQidCache;
    this.uacService = uacService;
    this.eventLogger = eventLogger;
    this.exportFileRowRepository = exportFileRowRepository;
    this.rasRmCaseIacService = rasRmCaseIacService;
  }

  public void process(FulfilmentToProcess fulfilmentToProcess) {
    ExportFileTemplate exportFileTemplate = fulfilmentToProcess.getExportFileTemplate();

    processExportFileRow(
        exportFileTemplate.getTemplate(),
        fulfilmentToProcess.getCaze(),
        fulfilmentToProcess.getBatchId(),
        fulfilmentToProcess.getBatchQuantity(),
        exportFileTemplate.getPackCode(),
        exportFileTemplate.getExportFileDestination(),
        fulfilmentToProcess.getCorrelationId(),
        fulfilmentToProcess.getOriginatingUser(),
        fulfilmentToProcess.getUacMetadata());
  }

  public void processExportFileRow(
      String[] template,
      Case caze,
      UUID batchId,
      int batchQuantity,
      String packCode,
      String exportFileDestination,
      UUID correlationId,
      String originatingUser,
      Object uacMetadata) {

    UacQidDTO uacQidDTO = null;
    String[] rowStrings = new String[template.length];

    for (int i = 0; i < template.length; i++) {
      String templateItem = template[i];

      switch (templateItem) {
        case "__caseref__":
          rowStrings[i] = Long.toString(caze.getCaseRef());
          break;
        case "__uac__":
          if (uacQidDTO == null) {
            uacQidDTO = getUacQidForCase(caze, correlationId, originatingUser, uacMetadata);
          }

          rowStrings[i] = uacQidDTO.getUac();
          break;
        case "__qid__":
          if (uacQidDTO == null) {
            uacQidDTO = getUacQidForCase(caze, correlationId, originatingUser, uacMetadata);
          }

          rowStrings[i] = uacQidDTO.getQid();
          break;
        case "__ras_rm_iac__":
          rowStrings[i] = rasRmCaseIacService.getRasRmIac(caze);
          break;
        default:
          rowStrings[i] = caze.getSample().get(templateItem);
      }
    }

    ExportFileRow exportFileRow = new ExportFileRow();
    exportFileRow.setRow(getCsvRow(rowStrings));
    exportFileRow.setBatchId(batchId);
    exportFileRow.setBatchQuantity(batchQuantity);
    exportFileRow.setPackCode(packCode);
    exportFileRow.setExportFileDestination(exportFileDestination);

    exportFileRowRepository.save(exportFileRow);

    eventLogger.logCaseEvent(
        caze,
        String.format("Export file generated with pack code %s", packCode),
        EventType.EXPORT_FILE,
        EventHelper.getDummyEvent(correlationId, originatingUser),
        OffsetDateTime.now());
  }

  // Has to be synchronised to stop different threads from mangling writer buffer contents
  public synchronized String getCsvRow(String[] rowStrings) {
    csvWriter.writeNext(rowStrings);
    String csvRow = stringWriter.toString();
    stringWriter.getBuffer().delete(0, stringWriter.getBuffer().length()); // Reset the writer
    return csvRow;
  }

  private UacQidDTO getUacQidForCase(
      Case caze, UUID correlationId, String originatingUser, Object metadata) {

    CollectionInstrumentSelectionRule[] collectionInstrumentSelectionRules = new CollectionInstrumentSelectionRule[] {
        new CollectionInstrumentSelectionRule(1000, "caze.sample['POSTCODE'] = 'peter'", "http://brian/andrew"),
        new CollectionInstrumentSelectionRule(500, "caze.sample['POSTCODE'] = 'john'", "http://norman/george"),
        new CollectionInstrumentSelectionRule(0, null, "http://thomas/ermintrude")
    };

    EvaluationBundle bundle = new EvaluationBundle(caze);
    EvaluationContext context = new StandardEvaluationContext(bundle);

    ExpressionParser expressionParser = new SpelExpressionParser();
    String selectedUrl = null;
    int selectedPriority = Integer.MIN_VALUE;
    for (CollectionInstrumentSelectionRule rule : collectionInstrumentSelectionRules) {
      if (rule.getPriority() < selectedPriority) {
        // If the priority of the rule is lower than a rule that has matched, then ignore the
        // rule, because another one is higher priority so we should use that one
        continue;
      }

      Boolean expressionResult = Boolean.TRUE;

      // No expression means "match anything"... used for 'default' rule
      if (rule.getSpelExpression() != null) {
        Expression spelExpression = expressionParser.parseExpression(rule.getSpelExpression());
        expressionResult = spelExpression.getValue(context, Boolean.class);
      }

      if (expressionResult) {
        selectedPriority = rule.getPriority();
        selectedUrl = rule.getCollectionInstrumentUrl();
      }
    }

    System.out.println("Would have used this instrument: " + selectedUrl);

    UacQidDTO uacQidDTO = uacQidCache.getUacQidPair(1);
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(uacQidDTO.getQid());
    uacQidLink.setUac(uacQidDTO.getUac());
    uacQidLink.setMetadata(metadata);
    uacQidLink.setCaze(caze);
    uacService.saveAndEmitUacUpdateEvent(uacQidLink, correlationId, originatingUser);

    return uacQidDTO;
  }
}