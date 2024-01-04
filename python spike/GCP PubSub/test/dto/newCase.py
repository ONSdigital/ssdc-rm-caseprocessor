from dataclasses import dataclass
import uuid


@dataclass
class NewCase:
    case_id: uuid
    collection_exercise_id: uuid
    sample: {}
    sample_sensitive: {}
