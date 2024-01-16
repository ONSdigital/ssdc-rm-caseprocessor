"""
This is just temporary for the spike - since common entity is out of scope

This will get replaced if and when we change common entity to python
"""
from dataclasses import dataclass
import uuid
from .collection_exercise import CollectionExercise
from typing import Dict
from sqlalchemy import func
from datetime import datetime


@dataclass
class Case:
    id: uuid
    collection_exercise_id: CollectionExercise
    sample: Dict[str, str]
    sample_sensitive: Dict[str, str]
    created_at: datetime
    last_updated_at: datetime
    invalid: bool = False
    case_ref: int = None
    secret_sequence_number: int = None # Not 100% sure if this is what we want

