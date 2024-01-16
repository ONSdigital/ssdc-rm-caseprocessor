"""
This is just temporary for the spike - since common entity is out of scope

This will get replaced if and when we change common entity to python
"""
from dataclasses import dataclass
import uuid
from .collection_exercise import CollectionExercise
from typing import Dict
from sqlalchemy import func


@dataclass
class Case:
    id: uuid
    collection_exercise_id: CollectionExercise
    sample: Dict[str, str]
    sample_sensitive: Dict[str, str]
    case_ref: int = None
    secret_sequence_number: int = func.now()  # Not 100% sure if this is what we want
