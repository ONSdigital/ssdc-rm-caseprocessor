import json
from dataclass_wizard import fromdict
from caseprocessor.config import PubsubConfig
from caseprocessor.dto.event_dto import EventDTO
from caseprocessor.db.case_repository import CasesTable
from caseprocessor.db.collection_exercise_repository import CollectionExerciseTable
from caseprocessor.db.db_utility import create_session
from caseprocessor.validation.column_validator import ColumnValidator
from caseprocessor.entity.case import Case
from caseprocessor.case_ref_generator import get_case_ref

class NewCaseReceiver:
    __case_ref_generator_key = bytes("abc123", "utf-8")

    @classmethod
    def receive_new_case(cls, message: bytes):
        try:
            parsed_json = json.loads(message)
            event = fromdict(EventDTO, parsed_json)

            new_case_payload = event.payload.new_case

            print("----Json Deserialized----")
            print(new_case_payload)

            session = create_session()

            if CasesTable.exists_by_id(session, new_case_payload.case_id):
                return

            collex = CollectionExerciseTable.find_by_id(session, new_case_payload.collection_exercise_id)

            if collex is None:
                raise Exception("Collection exercise '"
                                + new_case_payload.collection_exercise_id()
                                + "' not found")

            column_validator = collex.survey.sample_validation_rules

            cls.__check_new_sensitive_within_sample_sensitive_definition(column_validator, new_case_payload)
            cls.__check_new_sample_within_sample_definition(column_validator, new_case_payload)
            cls.__validate_new_case(column_validator, new_case_payload)

            sample = new_case_payload.sample

            new_case = Case(
                case_id=new_case_payload.case_id,
                collection_exercise=collex,
                sample=new_case_payload.sample,
                sample_sensitive=new_case_payload.sample_sensitive
            )

            new_case = cls.__save_new_case_and_stamp_case_ref(new_case)

            # TODO: add logger and finish this off



        except TypeError as e:
            # TODO: throw error if json is wrong/can't cast to class
            print(f"something went wrong:\n{e}")

    @classmethod
    def __check_new_sensitive_within_sample_sensitive_definition(cls, column_validators, new_case_payload):
        sensitive_columns = {ColumnValidator.get_column_name(column_validator)
                             for column_validator in column_validators
                             if ColumnValidator.is_sensitive(column_validator)}

        if sensitive_columns.issubset(new_case_payload.sample_sensitive.keys()):
            raise Exception("Attempt to send sensitive data to RM which was not part of defined sample")

        return sensitive_columns

    @classmethod
    def __check_new_sample_within_sample_definition(cls, column_validators, new_case_payload):
        sensitive_columns = {ColumnValidator.get_column_name(column_validator)
                             for column_validator in column_validators
                             if not ColumnValidator.is_sensitive(column_validator)}

        if sensitive_columns.issubset(new_case_payload.sample_sensitive.keys()):
            raise Exception("Attempt to send data to RM which was not part of defined sample")

        return sensitive_columns

    @classmethod
    def __validate_new_case(cls, column_validators, new_case_payload):
        validate_errors = []

        for column_validator in column_validators:
            if column_validator.is_sensitive:
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

    @classmethod
    def __save_new_case_and_stamp_case_ref(cls, caze, session):
        CasesTable.save_and_flush(session, caze)
        caze.case_ref = get_case_ref(caze.secret_sequence_number(), cls.__case_ref_generator_key)
        return caze
