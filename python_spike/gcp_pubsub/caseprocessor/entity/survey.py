"""
This is just temporary for the spike - since common entity is out of scope

This will get replaced if and when we change common entity to python
"""
from dataclasses import dataclass
from validation.column_validator import ColumnValidator
from typing import List
import uuid


@dataclass
class Survey:
    id: uuid
    sample_validation_rules: List[ColumnValidator]