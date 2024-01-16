from dataclasses import dataclass
from typing import Dict
import uuid
from dataclasses_json import LetterCase, dataclass_json


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class NewCase:
    case_id: uuid
    collection_exercise_id: uuid
    sample: Dict[str, str]
    sample_sensitive: Dict[str, str]
