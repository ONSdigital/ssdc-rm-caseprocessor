package uk.gov.ons.ssdc.caseprocessor.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.dto.PrintRow;
import uk.gov.ons.ssdc.caseprocessor.model.entity.CaseToProcess;

@Component
public class CaseProcessor {
  private final RabbitTemplate rabbitTemplate;

  public CaseProcessor(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void process(CaseToProcess caseToProcess) {
    String row = "";
    for (String templateItem : caseToProcess.getWaveOfContact().getTemplate()) {
      if (!row.isEmpty()) {
        row += "|";
      }

      row += caseToProcess.getCaze().getSample().get(templateItem);
    }

    PrintRow printRow = new PrintRow();

    System.out.println("Processing case: "+ caseToProcess.getCaze().getId());
  }
}
