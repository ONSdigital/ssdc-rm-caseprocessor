package uk.gov.ons.ssdc.caseprocessor.service;

import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.Case;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@Component
public class CaseToProcessProcessor {
  private final RabbitTemplate rabbitTemplate;
  private final UacQidCache uacQidCache;
  private final UacQidLinkRepository uacQidLinkRepository;

  private final StringWriter stringWriter = new StringWriter();
  private final CSVWriter csvWriter =
      new CSVWriter(
          stringWriter,
          '|',
          CSVWriter.DEFAULT_QUOTE_CHARACTER,
          CSVWriter.DEFAULT_ESCAPE_CHARACTER,
          "");

  @Value("${queueconfig.print-queue}")
  private String printQueue;

  public CaseToProcessProcessor(
      RabbitTemplate rabbitTemplate,
      UacQidCache uacQidCache,
      UacQidLinkRepository uacQidLinkRepository) {
    this.rabbitTemplate = rabbitTemplate;
    this.uacQidCache = uacQidCache;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  public void process(CaseToProcess caseToProcess) {
    if (caseToProcess.getWaveOfContact().getType() == WaveOfContactType.PRINT) {
      processPrintCase(caseToProcess);
    }
  }

  private void processPrintCase(CaseToProcess caseToProcess) {

    UacQidDTO uacQidDTO = null;
    String[] rowStrings = new String[caseToProcess.getWaveOfContact().getTemplate().length];

    for (int i = 0; i < caseToProcess.getWaveOfContact().getTemplate().length; i++) {
      String templateItem = caseToProcess.getWaveOfContact().getTemplate()[i];

      switch (templateItem) {
        case "__caseref__":
          rowStrings[i] = Long.toString(caseToProcess.getCaze().getCaseRef());
          break;
        case "__uac__":
          if (uacQidDTO == null) {
            uacQidDTO = getUacQidForCase(caseToProcess.getCaze());
          }

          rowStrings[i] = uacQidDTO.getUac();
          break;
        case "__qid__":
          if (uacQidDTO == null) {
            uacQidDTO = getUacQidForCase(caseToProcess.getCaze());
          }

          rowStrings[i] = uacQidDTO.getQid();
          break;
        default:
          rowStrings[i] = caseToProcess.getCaze().getSample().get(templateItem);
      }
    }

    PrintRow printRow = new PrintRow();
    printRow.setRow(getCsvRow(rowStrings));
    printRow.setBatchId(caseToProcess.getBatchId());
    printRow.setBatchQuantity(caseToProcess.getBatchQuantity());
    printRow.setPackCode(caseToProcess.getWaveOfContact().getPackCode());
    printRow.setPrintSupplier(caseToProcess.getWaveOfContact().getPrintSupplier());

    rabbitTemplate.convertAndSend("", printQueue, printRow);
  }

  // Has to be synchronised to stop different threads from mangling writer buffer contents
  private synchronized String getCsvRow(String[] rowStrings) {
    csvWriter.writeNext(rowStrings);
    String csvRow = stringWriter.toString();
    stringWriter.getBuffer().delete(0, stringWriter.getBuffer().length()); // Reset the writer
    return csvRow;
  }

  private UacQidDTO getUacQidForCase(Case caze) {
    UacQidDTO uacQidDTO = uacQidCache.getUacQidPair(1);
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(uacQidDTO.getQid());
    uacQidLink.setUac(uacQidDTO.getUac());
    uacQidLink.setCaze(caze);
    uacQidLinkRepository.saveAndFlush(uacQidLink);
  }
}
