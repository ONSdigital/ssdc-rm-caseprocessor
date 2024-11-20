"""
This is just temporary for the spike - since common validation is out of scope

This will get replaced if and when we change common validation to python
"""
from abc import ABC, abstractmethod
from typing import Optional


# Direct translation from java -> not sure if it's very pythonic
class Rule(ABC):
    @abstractmethod
    def check_validity(self, data: str) -> Optional[str]:
        pass
