from caseprocessor.pseudorandom.pseudo_random_number_generator import get_pseudorandom
from caseprocessor.pseudorandom.luhn_utility import add_check_digit
from config import CaseRegGeneratorConfig


def get_case_ref(sequence_number: int, case_ref_generator_key: bytes) -> int:
    pseudo_random_number = get_pseudorandom(sequence_number, case_ref_generator_key)

    case_ref_without_check_digit = int(pseudo_random_number + CaseRegGeneratorConfig.LOWEST_POSSIBLE_CASE_REF)
    return int(add_check_digit(case_ref_without_check_digit))
