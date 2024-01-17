from caseprocessor.entity.case import Case
from caseprocessor.dto.event_header_dto import EventHeaderDTO
from caseprocessor.dto.case_update_dto import CaseUpdateDTO
from caseprocessor.entity.collection_exercise import CollectionExercise
from caseprocessor.dto.event_dto import EventDTO
from caseprocessor.dto.payload_dto import PayloadDTO
from caseprocessor.util.redact_helper import redact
from config import PubsubConfig

import uuid

EVENT_SOURCE = "CASE_PROCESSOR"
EVENT_CHANNEL = "RM"

CASE_UPDATE_TOPIC = ""


def emit_case(caze: Case, correlation_id: uuid, originating_user: str, collex: CollectionExercise):
    event_header = EventHeaderDTO(
        topic=PubsubConfig.CASE_UPDATE_TOPIC,
        channel=EVENT_CHANNEL,
        source=EVENT_SOURCE,
        correlation_id=correlation_id,
        originating_user=originating_user
    )

    event = __prepare_case_event(caze, event_header, collex)
    event_json = event.to_json()
    future = PubsubConfig.PUBLISHER.publish(PubsubConfig.CASE_UPDATE_PATH, event_json.encode('utf-8'))
    future.result()


def __prepare_case_event(caze, event_header, collex):
    case_update = CaseUpdateDTO(
        case_id=caze.id,
        case_ref=str(caze.case_ref),
        collection_exercise_id=caze.collection_exercise_id,
        sample=caze.sample,
        sample_sensitive=caze.sample_sensitive,
        created_at=caze.created_at,
        last_updated_at=caze.last_updated_at,
        invalid=caze.invalid,
        survey_id=collex.survey.id
    )

    if case_update.sample_sensitive:
        case_update = redact(case_update)

    return EventDTO(
        header=event_header,
        payload=PayloadDTO(case_update_dto=case_update)
    )
