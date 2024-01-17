import json
from .dto.event_dto import EventDTO
from .db.case_table_util import *
from .db.collection_exercise_table_util import CollectionExerciseTable, find_collex_by_id
from .db.db_utility import create_session
from .validation.column_validator import ColumnValidator
from .util.case_ref_generator import get_case_ref
from .entity.case import Case
from .service.case_service import emit_case
from .logging.event_logging import log_case_event
from .entity.event_type import EventType
from sqlalchemy import func

__case_ref_generator_key = bytes("abc123", "utf-8")

NEW_CLASS_LOG_MSG = "New Case created"


def receive_new_case(message: bytes):
    event = EventDTO.from_json(message)
    new_case_payload = event.payload.new_case

    session = create_session()

    if exists_by_id(session, new_case_payload.case_id):
        return

    collex = find_collex_by_id(session, new_case_payload.collection_exercise_id)

    if collex is None:
        raise Exception("Collection exercise '"
                        + new_case_payload.collection_exercise_id
                        + "' not found")

    column_validator = collex.survey.sample_validation_rules

    __check_new_sensitive_within_sample_sensitive_definition(column_validator, new_case_payload)
    __check_new_sample_within_sample_definition(column_validator, new_case_payload)
    __validate_new_case(column_validator, new_case_payload)

    new_case = Case(
        id=new_case_payload.case_id,
        collection_exercise_id=collex.id,
        sample=new_case_payload.sample,
        sample_sensitive=new_case_payload.sample_sensitive,
        created_at=func.current_timestamp()
    )

    new_case = __save_new_case_and_stamp_case_ref(new_case, session)

    emit_case(new_case, event.header.correlation_id, event.header.originating_user, collex)

    log_case_event(session, NEW_CLASS_LOG_MSG, EventType.NEW_CASE, event, new_case.created_at)

    session.commit()

    print("it worked")


def __check_new_sensitive_within_sample_sensitive_definition(column_validators, new_case_payload):
    sensitive_columns = {ColumnValidator.get_column_name(column_validator)
                         for column_validator in column_validators
                         if ColumnValidator.is_sensitive(column_validator)}

    if not sensitive_columns.issubset(new_case_payload.sample_sensitive.keys()):
        raise Exception("Attempt to send sensitive data to RM which was not part of defined sample")

    return sensitive_columns


def __check_new_sample_within_sample_definition(column_validators, new_case_payload):
    non_sensitive_columns = {ColumnValidator.get_column_name(column_validator)
                             for column_validator in column_validators
                             if not ColumnValidator.is_sensitive(column_validator)}

    if not non_sensitive_columns.issubset(new_case_payload.sample.keys()):
        raise Exception("Attempt to send data to RM which was not part of defined sample")

    return non_sensitive_columns


def __validate_new_case(column_validators, new_case_payload):
    validate_errors = []

    for column_validator in column_validators:
        if column_validator.is_sensitive():
            error = column_validator.validate_row(new_case_payload.sample_sensitive,
                                                  exclude_data_from_returned_error_msgs=True)
            if error is not None:
                validate_errors.append(error)
        else:
            error = column_validator.validate_row(new_case_payload.sample,
                                                  exclude_data_from_returned_error_msgs=True)
            if error is not None:
                validate_errors.append(error)

    if validate_errors:
        raise Exception("NEW_CASE event: " + "\n.join(validate_errors)")


def __save_new_case_and_stamp_case_ref(caze: Case, session):
    add_and_flush(session, caze)
    caze.case_ref = get_case_ref(caze.secret_sequence_number, __case_ref_generator_key)
    update_case_ref(session, caze)
    return caze
