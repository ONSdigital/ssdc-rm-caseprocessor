import uuid
from dataclasses import dataclass
from datetime import datetime


@dataclass
class EventHeaderDTO:
    version: str
    topic: str
    source: str
    channel: str
    datetime: datetime
    message_id: uuid
    correlation_id: uuid
    originating_user: str
