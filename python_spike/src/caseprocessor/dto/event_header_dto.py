import uuid
from dataclasses import dataclass, field
from datetime import datetime
from dataclasses_json import LetterCase, dataclass_json, config
from marshmallow import fields
from typing import Optional


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class EventHeaderDTO:
    topic: str
    source: str
    channel: str
    correlation_id: uuid
    originating_user: str
    date_time: Optional[datetime] = field(
        default=None,
        metadata=config(
            encoder=lambda time: datetime.isoformat(time) if time else None,
            decoder=lambda time: datetime.fromisoformat(time) if time else None,
            mm_field=fields.DateTime(format='iso')
        )
    )
    message_id: uuid = None
    version: str = None
