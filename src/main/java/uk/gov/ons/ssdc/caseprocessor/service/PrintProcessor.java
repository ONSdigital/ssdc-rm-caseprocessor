package uk.gov.ons.ssdc.caseprocessor.service;

import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.logging.EventLogger;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.*;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FileRowRepository;
import uk.gov.ons.ssdc.caseprocessor.utils.EventHelper;

@Component
public class PrintProcessor {
  private final MessageSender messageSender;
  private final UacQidCache uacQidCache;
  private final UacService uacService;
  private final EventLogger eventLogger;
  private final FileRowRepository fileRowRepository;

  private final StringWriter stringWriter = new StringWriter();
  private final CSVWriter csvWriter =
      new CSVWriter(
          stringWriter,
          '|',
          CSVWriter.DEFAULT_QUOTE_CHARACTER,
          CSVWriter.DEFAULT_ESCAPE_CHARACTER,
          "");

  @Value("${queueconfig.print-topic}")
  private String printTopic;

  public PrintProcessor(
      MessageSender messageSender,
      UacQidCache uacQidCache,
      UacService uacService,
      EventLogger eventLogger,
      FileRowRepository fileRowRepository) {
    this.messageSender = messageSender;
    this.uacQidCache = uacQidCache;
    this.uacService = uacService;
    this.eventLogger = eventLogger;
    this.fileRowRepository = fileRowRepository;
  }

  public void process(FulfilmentToProcess fulfilmentToProcess) {
    PrintTemplate printTemplate = fulfilmentToProcess.getPrintTemplate();

    processPrintRow(
        printTemplate.getTemplate(),
        fulfilmentToProcess.getCaze(),
        fulfilmentToProcess.getBatchId(),
        fulfilmentToProcess.getBatchQuantity(),
        printTemplate.getPackCode(),
        printTemplate.getPrintSupplier());
  }

  public void processPrintRow(
      String[] template,
      Case caze,
      UUID batchId,
      int batchQuantity,
      String packCode,
      String printSupplier) {

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
            uacQidDTO = getUacQidForCase(caze);
          }

          rowStrings[i] = uacQidDTO.getUac();
          break;
        case "__qid__":
          if (uacQidDTO == null) {
            uacQidDTO = getUacQidForCase(caze);
          }

          rowStrings[i] = uacQidDTO.getQid();
          break;
        default:
          rowStrings[i] = caze.getSample().get(templateItem);
      }
    }

    FileRow fileRow = new FileRow();
    fileRow.setRow(getCsvRow(rowStrings));
    fileRow.setBatchId(batchId);
    fileRow.setBatchQuantity(batchQuantity);
    fileRow.setPackCode(packCode);
    fileRow.setPrintSupplier(printSupplier);

    fileRowRepository.save(fileRow);

    eventLogger.logCaseEvent(
        caze,
        OffsetDateTime.now(),
        String.format("Print file generated with pack code %s", packCode),
        EventType.PRINT_FILE,
        EventHelper.getDummyEvent(),
        null,
        OffsetDateTime.now());
  }

  // Has to be synchronised to stop different threads from mangling writer buffer contents
  public synchronized String getCsvRow(String[] rowStrings) {
    csvWriter.writeNext(rowStrings);
    String csvRow = stringWriter.toString();
    stringWriter.getBuffer().delete(0, stringWriter.getBuffer().length()); // Reset the writer
    return csvRow;
  }

  public UacQidDTO getUacQidForCase(Case caze) {
    UacQidDTO uacQidDTO = uacQidCache.getUacQidPair(1);
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(uacQidDTO.getQid());
    uacQidLink.setUac(uacQidDTO.getUac());
    uacQidLink.setCaze(caze);
    uacService.saveAndEmitUacUpdateEvent(uacQidLink);

    return uacQidDTO;
  }
}
