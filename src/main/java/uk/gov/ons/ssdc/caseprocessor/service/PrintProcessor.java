package uk.gov.ons.ssdc.caseprocessor.service;

import com.opencsv.CSVWriter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.*;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentTemplateRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

import java.io.StringWriter;
import java.util.Optional;
import java.util.UUID;

@Component
public class PrintProcessor {
  public final RabbitTemplate rabbitTemplate;
  public final UacQidCache uacQidCache;
  public final UacQidLinkRepository uacQidLinkRepository;
  public final UacService uacService;
  private final FulfilmentTemplateRepository fulfilmentTemplateRepository;

  public final StringWriter stringWriter = new StringWriter();
  public final CSVWriter csvWriter =
      new CSVWriter(
          stringWriter,
          '|',
          CSVWriter.DEFAULT_QUOTE_CHARACTER,
          CSVWriter.DEFAULT_ESCAPE_CHARACTER,
          "");

  @Value("${queueconfig.print-queue}")
  public String printQueue;

  public PrintProcessor(
      RabbitTemplate rabbitTemplate,
      UacQidCache uacQidCache,
      UacQidLinkRepository uacQidLinkRepository,
      UacService uacService,
      FulfilmentTemplateRepository fulfilmentTemplateRepository) {
    this.rabbitTemplate = rabbitTemplate;
    this.uacQidCache = uacQidCache;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.uacService = uacService;
    this.fulfilmentTemplateRepository = fulfilmentTemplateRepository;
  }

  public void process(FulfilmentToProcess fulfilmentToProcess) {
    Optional<FulfilmentTemplate> fulfilmentTemplateOpt =
            fulfilmentTemplateRepository.findById(fulfilmentToProcess.getFulfilmentCode());
    if (!fulfilmentTemplateOpt.isPresent()) {
      throw new RuntimeException(
              String.format(
                      "No template for fulfilment code %s", fulfilmentToProcess.getFulfilmentCode()));
    }

    FulfilmentTemplate fulfilmentTemplate = fulfilmentTemplateOpt.get();

    processPrintRow(fulfilmentTemplate.getTemplate(),
            fulfilmentToProcess.getCaze(),
            fulfilmentToProcess.getBatchId(),
            fulfilmentToProcess.getBatchQuantity(),
            fulfilmentToProcess.getFulfilmentCode(),
            fulfilmentTemplate.getPrintSupplier());
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

    PrintRow printRow = new PrintRow();
    printRow.setRow(getCsvRow(rowStrings));
    printRow.setBatchId(batchId);
    printRow.setBatchQuantity(batchQuantity);
    printRow.setPackCode(packCode);
    printRow.setPrintSupplier(printSupplier);

    rabbitTemplate.convertAndSend("", printQueue, printRow);
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
    uacQidLinkRepository.saveAndFlush(uacQidLink);

    uacService.saveAndEmitUacUpdatedEvent(uacQidLink);

    return uacQidDTO;
  }
}
