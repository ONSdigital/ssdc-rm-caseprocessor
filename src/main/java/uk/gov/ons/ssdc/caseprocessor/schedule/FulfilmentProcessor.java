package uk.gov.ons.ssdc.caseprocessor.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.model.repository.FulfilmentToProcessRepository;

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
    List<String> packCodes = fulfilmentToProcessRepository.findDistinctPackCode();

    packCodes.forEach(
        packCode -> {
          UUID batchId = UUID.randomUUID();
          log.with("batch_id", batchId).with("pack_code", packCode).info("Fulfilments triggered");

          jdbcTemplate.update(
              "UPDATE casev3.fulfilment_to_process "
                  + "SET batch_quantity = (SELECT COUNT(*) FROM casev3.fulfilment_to_process "
                  + "WHERE export_file_template_pack_code = ?), batch_id = ? WHERE export_file_template_pack_code = ?",
              packCode,
              batchId,
              packCode);
        });
  }
}
