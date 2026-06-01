package uk.gov.ons.ssdc.caseprocessor.service;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.ssdc.caseprocessor.messaging.MessageSender;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.CaseToProcessRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.EventRepository;
import uk.gov.ons.ssdc.caseprocessor.model.repository.UacQidLinkRepository;

@Component
public class RemovePersonalDataProcessor {
  private final MessageSender messageSender;
  private final EventRepository eventRepository;
  private final UacQidLinkRepository uacQidLinkRepository;
  private final CaseRepository caseRepository;
  private final CaseToProcessRepository caseToProcessRepository;
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  public RemovePersonalDataProcessor(
      MessageSender messageSender,
      EventRepository eventRepository,
      UacQidLinkRepository uacQidLinkRepository,
      CaseRepository caseRepository,
      CaseToProcessRepository caseToProcessRepository,
      JdbcTemplate jdbcTemplate,
      NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
    this.messageSender = messageSender;
    this.eventRepository = eventRepository;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.caseRepository = caseRepository;
    this.caseToProcessRepository = caseToProcessRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
  }

  public void process(List<UUID> caseIds) {
    deletePersonalDataFromDB(caseIds);
  }

  private void deletePersonalDataFromDB(List<UUID> caseIds) {
    if (caseIds.isEmpty()) return;
    // Deleting using jdbc to save memory since it saves loading all the models into memory

    MapSqlParameterSource params = new MapSqlParameterSource("ids", caseIds);

    namedParameterJdbcTemplate.update(
        """
                    DELETE FROM casev3.event
                    WHERE caze_id IN (:ids)
            """,
        params);

    namedParameterJdbcTemplate.update(
        """
                    DELETE FROM casev3.event
                    USING casev3.uac_qid_link uql
                    WHERE casev3.event.uac_qid_link_id = uql.id
                    AND uql.caze_id IN (:ids)
                    """,
        params);

    namedParameterJdbcTemplate.update(
        """
                    DELETE FROM casev3.uac_qid_link
              WHERE caze_id IN (:ids)
            """,
        params);

    namedParameterJdbcTemplate.update(
        """
                    DELETE FROM casev3.case_to_process
                    WHERE caze_id IN (:ids)
            """,
        params);

    namedParameterJdbcTemplate.update(
        """
                    DELETE FROM casev3.cases
                    WHERE id IN (:ids)
            """,
        params);
  }
}
