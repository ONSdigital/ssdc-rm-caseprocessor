package uk.gov.ons.ssdc.caseprocessor.service;

import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.repository.PrintFileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;
import uk.gov.ons.ssdc.common.model.entity.*;

@Component
public class PrintProcessor {
  private final MessageSender messageSender;
  private final UacQidCache uacQidCache;
  private final UacService uacService;
  private final EventLogger eventLogger;
  private final PrintFileRowRepository printFileRowRepository;

  private final StringWriter stringWriter = new StringWriter();
  private final CSVWriter csvWriter =
      new CSVWriter(
          stringWriter,
          '|',
          CSVWriter.DEFAULT_QUOTE_CHARACTER,
          CSVWriter.DEFAULT_ESCAPE_CHARACTER,
          "");

  public PrintProcessor(
      MessageSender messageSender,
      UacQidCache uacQidCache,
      UacService uacService,
      EventLogger eventLogger,
      PrintFileRowRepository printFileRowRepository) {
    this.messageSender = messageSender;
    this.uacQidCache = uacQidCache;
    this.uacService = uacService;
    this.eventLogger = eventLogger;
    this.printFileRowRepository = printFileRowRepository;
  }

  public void process(FulfilmentToProcess fulfilmentToProcess) {
    PrintTemplate printTemplate = fulfilmentToProcess.getPrintTemplate();

    processPrintRow(
        printTemplate.getTemplate(),
        fulfilmentToProcess.getCaze(),
        fulfilmentToProcess.getBatchId(),
        fulfilmentToProcess.getBatchQuantity(),
        printTemplate.getPackCode(),
        printTemplate.getPrintSupplier(),
        fulfilmentToProcess.getCorrelationId(),
        fulfilmentToProcess.getOriginatingUser(),
        fulfilmentToProcess.getUacMetadata());
  }

  public void processPrintRow(
      String[] template,
      Case caze,
      UUID batchId,
      int batchQuantity,
      String packCode,
      String printSupplier,
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
        default:
          rowStrings[i] = caze.getSample().get(templateItem);
      }
    }

    PrintFileRow printFileRow = new PrintFileRow();
    printFileRow.setRow(getCsvRow(rowStrings));
    printFileRow.setBatchId(batchId);
    printFileRow.setBatchQuantity(batchQuantity);
    printFileRow.setPackCode(packCode);
    printFileRow.setPrintSupplier(printSupplier);

    printFileRowRepository.save(printFileRow);

    eventLogger.logCaseEvent(
        caze,
        String.format("Print file generated with pack code %s", packCode),
        EventType.PRINT_FILE,
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

  public UacQidDTO getUacQidForCase(
      Case caze, UUID correlationId, String originatingUser, Object metadata) {
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
