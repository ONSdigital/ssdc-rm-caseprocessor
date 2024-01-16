from dataclasses import dataclass
import uuid
from datetime import datetime
from typing import Dict
from .refusal_type import RefusalType
from dataclasses_json import LetterCase, dataclass_json


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class CaseUpdateDTO:
    case_id: uuid
    case_ref: str
    collection_exercise_id: uuid
    invalid: bool
    sample: Dict[str, str]
    created_at: datetime
    last_updated_at: datetime
    refusal_received: RefusalType = None
    sample_sensitive: Dict[str, str] = None
    survey_id: uuid = None
