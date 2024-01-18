from dataclasses import dataclass, field
from .newCase import NewCase
from .case_update_dto import CaseUpdateDTO
from dataclasses_json import LetterCase, dataclass_json, config
from typing import Optional


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class PayloadDTO:
    new_case: Optional[NewCase] = field(default=None, metadata=config(exclude=lambda x: x is None))
    case_update: Optional[CaseUpdateDTO] = None
