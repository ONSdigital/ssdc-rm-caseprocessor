package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.cache.UacQidCache;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.dto.UacQidDTO;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;
import uk.gov.ons.ssdc.caseprocessor.model.entity.UacQidLink;
import uk.gov.ons.ssdc.caseprocessor.model.entity.WaveOfContactType;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@Component
public class CaseProcessor {
  private final RabbitTemplate rabbitTemplate;
  private final UacQidCache uacQidCache;
  private final UacQidLinkRepository uacQidLinkRepository;

  public CaseProcessor(RabbitTemplate rabbitTemplate,
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

    System.out.println("Processed case: " + caseToProcess.getCaze().getId());
  }

  private void processPrintCase(CaseToProcess caseToProcess) {

    String row = "";
    for (String templateItem : caseToProcess.getWaveOfContact().getTemplate()) {
      if (!row.isEmpty()) {
        row += "|";
      }

      if (templateItem.equals("__caseref__")) {
        row += caseToProcess.getCaze().getCaseRef();
      } else if (templateItem.equals("__uac__")) {
        UacQidDTO uacQidDTO = uacQidCache.getUacQidPair(1);
        UacQidLink uacQidLink = new UacQidLink();
        uacQidLink.setId(UUID.randomUUID());
        uacQidLink.setQid(uacQidDTO.getQid());
        uacQidLink.setUac(uacQidDTO.getUac());
        uacQidLink.setCaze(caseToProcess.getCaze());
        uacQidLinkRepository.saveAndFlush(uacQidLink);

        row += uacQidDTO.getUac();
      } else {
        row += caseToProcess.getCaze().getSample().get(templateItem);
      }
    }

    PrintRow printRow = new PrintRow();
    printRow.setRow(row);
    printRow.setBatchId(caseToProcess.getBatchId());
    printRow.setBatchQuantity(caseToProcess.getBatchQuantity());
    printRow.setPackCode(caseToProcess.getWaveOfContact().getPackCode());
    printRow.setPrintSupplier(caseToProcess.getWaveOfContact().getPrintSupplier());

    rabbitTemplate.convertAndSend("", "Action.Printer", printRow);
  }
}
