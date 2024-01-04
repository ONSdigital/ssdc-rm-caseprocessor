from dataclasses import dataclass
from EventHeaderDTO import EventHeaderDTO
from PayloadDTO import PayloadDTO


@dataclass
class EventDTO:
    header: EventHeaderDTO
    payload: PayloadDTO

