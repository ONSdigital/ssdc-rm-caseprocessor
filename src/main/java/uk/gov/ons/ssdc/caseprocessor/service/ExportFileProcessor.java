package uk.gov.ons.ssdc.caseprocessor.service;

import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.REQUEST_PERSONALISATION_PREFIX;
import static uk.gov.ons.ssdc.caseprocessor.utils.Constants.SENSITIVE_FIELD_PREFIX;

import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.collectioninstrument.CollectionInstrumentHelper;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.model.dto.ExportFileDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PayloadDTO;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.ExportFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.caseprocessor.utils.HashHelper;
import uk.gov.ons.ssdc.common.model.entity.*;

@Component
public class ExportFileProcessor {
  private final UacQidCache uacQidCache;
  private final UacService uacService;
  private final EventLogger eventLogger;
  private final ExportFileRowRepository exportFileRowRepository;
  private final CollectionInstrumentHelper collectionInstrumentHelper;

  private final StringWriter stringWriter = new StringWriter();
  private final CSVWriter csvWriter =
      new CSVWriter(
          stringWriter,
          ',',
          CSVWriter.DEFAULT_QUOTE_CHARACTER,
          CSVWriter.DEFAULT_ESCAPE_CHARACTER,
          "");

  public ExportFileProcessor(
      UacQidCache uacQidCache,
      UacService uacService,
      EventLogger eventLogger,
      ExportFileRowRepository exportFileRowRepository,
      CollectionInstrumentHelper collectionInstrumentHelper) {
    this.uacQidCache = uacQidCache;
    this.uacService = uacService;
    this.eventLogger = eventLogger;
    this.exportFileRowRepository = exportFileRowRepository;
    this.collectionInstrumentHelper = collectionInstrumentHelper;
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
        fulfilmentToProcess.getUacMetadata(),
        fulfilmentToProcess.getPersonalisation());
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
    // Supply empty personalisation if the caller does not
    processExportFileRow(
        template,
        caze,
        batchId,
        batchQuantity,
        packCode,
        exportFileDestination,
        correlationId,
        originatingUser,
        uacMetadata,
        Map.of());
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
      Object uacMetadata,
      Map<String, String> personalisation) {

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
        default:
          if (templateItem.startsWith(REQUEST_PERSONALISATION_PREFIX)) {
            rowStrings[i] =
                personalisation != null
                    ? personalisation.get(
                        templateItem.substring(REQUEST_PERSONALISATION_PREFIX.length()))
                    : null;
          } else if (templateItem.startsWith(SENSITIVE_FIELD_PREFIX)) {
            rowStrings[i] =
                caze.getSampleSensitive()
                    .get(templateItem.substring(SENSITIVE_FIELD_PREFIX.length()));
          } else {
            rowStrings[i] = caze.getSample().get(templateItem);
          }
      }
    }

    ExportFileRow exportFileRow = new ExportFileRow();
    exportFileRow.setRow(getCsvRow(rowStrings));
    exportFileRow.setBatchId(batchId);
    exportFileRow.setBatchQuantity(batchQuantity);
    exportFileRow.setPackCode(packCode);
    exportFileRow.setExportFileDestination(exportFileDestination);

    exportFileRowRepository.save(exportFileRow);

    PayloadDTO exportFilePayload = new PayloadDTO();
    ExportFileDTO exportFileDTO = new ExportFileDTO(packCode);
    exportFilePayload.setExportFile(exportFileDTO);

    eventLogger.logCaseEvent(
        caze,
        String.format("Export file generated with pack code %s", packCode),
        EventType.EXPORT_FILE,
        EventHelper.getDummyEvent(correlationId, originatingUser, exportFilePayload),
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

    String collectionInstrumentUrl =
        collectionInstrumentHelper.getCollectionInstrumentUrl(caze, metadata);

    UacQidDTO uacQidDTO = uacQidCache.getUacQidPair(1);
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(uacQidDTO.getQid());
    uacQidLink.setUac(uacQidDTO.getUac());
    uacQidLink.setUacHash(HashHelper.hash(uacQidDTO.getUac()));
    uacQidLink.setMetadata(metadata);
    uacQidLink.setCaze(caze);
    uacQidLink.setCollectionInstrumentUrl(collectionInstrumentUrl);
    uacService.saveAndEmitUacUpdateEvent(uacQidLink, correlationId, originatingUser);

    return uacQidDTO;
  }
}
