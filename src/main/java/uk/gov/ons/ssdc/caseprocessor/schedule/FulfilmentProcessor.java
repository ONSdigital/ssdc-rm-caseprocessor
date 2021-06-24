package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Component
public class FulfilmentProcessor {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentProcessor.class);
  private final JdbcTemplate jdbcTemplate;
  private final FulfilmentToProcessRepository fulfilmentToProcessRepository;

  public FulfilmentProcessor(
      JdbcTemplate jdbcTemplate, FulfilmentToProcessRepository fulfilmentToProcessRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.fulfilmentToProcessRepository = fulfilmentToProcessRepository;
  }

  @Transactional
  public void addFulfilmentBatchIdAndQuantity() {
    List<String> fulfilmentCodes = fulfilmentToProcessRepository.findDistinctFulfilmentCode();

    fulfilmentCodes.forEach(
        fulfilmentCode -> {
          UUID batchId = UUID.randomUUID();
          log.with("batch_id", batchId)
              .with("fulfilment_code", fulfilmentCode)
              .info("Fulfilments triggered");

          jdbcTemplate.update(
              "UPDATE casev3.fulfilment_to_process "
                  + "SET quantity = (SELECT COUNT(*) FROM casev3.fulfilment_to_process "
                  + "WHERE fulfilment_code = ?), batch_id = ? WHERE fulfilment_code = ?",
              fulfilmentCode,
              batchId,
              fulfilmentCode);
        });
  }
}
