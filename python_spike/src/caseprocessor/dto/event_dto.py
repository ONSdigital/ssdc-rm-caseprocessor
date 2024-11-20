from dataclasses import dataclass
from .event_header_dto import EventHeaderDTO
from .payload_dto import PayloadDTO
from dataclasses_json import LetterCase, dataclass_json


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class EventDTO:
    header: EventHeaderDTO
    payload: PayloadDTO
