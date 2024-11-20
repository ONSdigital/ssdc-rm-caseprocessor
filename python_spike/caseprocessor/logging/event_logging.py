import uuid
from ..db.event_table import *
from ..dto.event_dto import EventDTO
from ..entity_models.event_type import EventType
from ..util.redact_helper import redact
from datetime import datetime
from sqlalchemy.orm import Session


def log_case_event(
        session: Session,
        event_description: str,
        event_type: EventType,
        event: EventDTO,
        message_timestamp: datetime):
    event_header = event.header
    payload = redact(event.payload)
    event_date = event_header.date_time

    logged_event = Event(
        id=uuid.uuid4(),
        date_time=event_date,
        description=event_description,
        type=event_type.name,
        channel=event_header.channel,
        source=event_header.source,
        message_id=event_header.message_id,
        message_timestamp=message_timestamp,
        created_by=event_header.originating_user,
        correlation_id=event_header.correlation_id,
        payload=payload.to_dict(),
        caze_id=payload.new_case.case_id
    )
    add_event(session, logged_event)
