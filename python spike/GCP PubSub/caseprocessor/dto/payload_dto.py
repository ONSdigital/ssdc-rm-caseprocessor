from dataclasses import dataclass
from .newCase import NewCase


@dataclass
class PayloadDTO:
    new_case: NewCase
