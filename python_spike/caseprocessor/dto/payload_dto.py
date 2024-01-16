from dataclasses import dataclass
from .newCase import NewCase
from .case_update_dto import CaseUpdateDTO
from dataclasses_json import LetterCase, dataclass_json
from typing import Optional


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class PayloadDTO:
    new_case: Optional[NewCase] = None
    case_update_dto: Optional[CaseUpdateDTO] = None
