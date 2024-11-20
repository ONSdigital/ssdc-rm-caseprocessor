from caseprocessor.entity_models.case import Case
from caseprocessor.dto.event_header_dto import EventHeaderDTO
from caseprocessor.dto.case_update_dto import CaseUpdateDTO
from caseprocessor.entity_models.collection_exercise import CollectionExercise
from caseprocessor.dto.event_dto import EventDTO
from caseprocessor.dto.payload_dto import PayloadDTO
from caseprocessor.util.redact_helper import redact
from caseprocessor.util.publish_util import publish_to_pubsub
from config import PubsubConfig
from datetime import datetime, timezone

import uuid

EVENT_SOURCE = "CASE_PROCESSOR"
EVENT_CHANNEL = "RM"

# In java repo this is in a constraints file, for now it's here
OUTBOUND_EVENT_SCHEMA_VERSION = '0.5.0'


def emit_case(caze: Case, correlation_id: uuid, originating_user: str, collex: CollectionExercise):
    event_header = EventHeaderDTO(
        topic=PubsubConfig.CASE_UPDATE_TOPIC,
        channel=EVENT_CHANNEL,
        source=EVENT_SOURCE,
        correlation_id=correlation_id,
        originating_user=originating_user,
        date_time=datetime.now(timezone.utc),
        message_id=uuid.uuid4(),
        version=OUTBOUND_EVENT_SCHEMA_VERSION
    )

    event = __prepare_case_event(caze, event_header, collex)
    event_json = event.to_json()
    publish_to_pubsub(PubsubConfig.CASE_UPDATE_TOPIC, event_json)


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
        payload=PayloadDTO(case_update=case_update)
    )
