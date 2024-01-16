from dataclasses import dataclass
from typing import Dict
import uuid


@dataclass
class NewCase:
    case_id: uuid
    collection_exercise_id: uuid
    sample: Dict[str, str]
    sample_sensitive: Dict[str, str]
