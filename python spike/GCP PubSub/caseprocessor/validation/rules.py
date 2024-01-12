"""
This is just temporary to test functionality
"""
from .rule import Rule
from typing import Optional


class EmailRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        pass


class InSetRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


class ISODateRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


class ISODateTimeRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


class LengthRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


class MandatoryRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


class NumericRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


class RegexRile(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


class UUIDRule(Rule):
    def check_validity(self, data: str) -> Optional[str]:
        return None


def create_rule(class_name: str) -> Rule:
    class_name = class_name[len("uk.gov.ons.ssdc.common.validation."):]
    match class_name:
        case "EmailRule":
            return EmailRule()
        case "InSetRule":
            return InSetRule()
        case "ISODateRule":
            return ISODateRule()
        case "LengthRule":
            return LengthRule()
        case "MandatoryRule":
            return MandatoryRule()
        case "NumericRule":
            return NumericRule()
        case "RegexRule":
            return RegexRile()
        case "UUIDRule":
            return UUIDRule()
        case _:
            raise Exception(f"Rule {class_name} doesn't exit")
