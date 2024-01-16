import uuid
from dataclasses import dataclass, field
from datetime import datetime
from dataclasses_json import LetterCase, dataclass_json, config
from marshmallow import fields
from typing import Optional

def date_time_decoder(time):
    if time is None:
        return None
    return datetime.fromisoformat(time)

def datetime_encoder(time):
    if time is None:
        return None
    return datetime.isoformat(time)

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
            encoder=datetime_encoder,
            decoder=date_time_decoder,
            mm_field=fields.DateTime(format='iso')
        )
    )
    message_id: uuid = None
    version: str = None

