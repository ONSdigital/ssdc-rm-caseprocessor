"""
This is just temporary for the spike - since common entity is out of scope

This will get replaced if and when we change common entity to python
"""
from dataclasses import dataclass
import uuid
from .survey import Survey


@dataclass
class CollectionExercise:
    id: uuid
    survey: Survey


