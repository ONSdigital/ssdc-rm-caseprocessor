"""
This is just temporary for the spike - since common entity_models is out of scope

This will get replaced if and when we change common entity_models to python
"""
from dataclasses import dataclass
from caseprocessor.validation.column_validator import ColumnValidator
from typing import List
import uuid


@dataclass
class Survey:
    id: uuid
    sample_validation_rules: List[ColumnValidator]