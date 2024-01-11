from dataclasses import dataclass
from .event_header_dto import EventHeaderDTO
from .payload_dto import PayloadDTO
import uuid
from datetime import datetime
from typing import Dict


@dataclass
class EventHeaderDTO:
    version: str
    topic: str
    source: str
    channel: str
    date_time: datetime
    message_id: uuid
    correlation_id: uuid
    originating_user: str


@dataclass
class NewCase:
    case_id: uuid
    collection_exercise_id: uuid
    sample: Dict[str, str]
    sample_sensitive: Dict[str, str]


@dataclass
class PayloadDTO:
    new_case: NewCase




@dataclass
class EventDTO:
    header: EventHeaderDTO
    payload: PayloadDTO

