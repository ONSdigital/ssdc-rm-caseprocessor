"""
Just using random for now for simplicity
"""

from .pseudorandom.pseudo_random_number_generator import PseudorandomNumberGenerator
from .pseudorandom.luhn_utility import add_check_digit

# This gives a caseRef 9 long, plus 1 checkbit at the end = 10
LOWEST_POSSIBLE_CASE_REF = 100000000
HIGHEST_POSSIBLE_CASE_REF = 999999999

PSEUDORANDOM_NUMBER_GENERATOR = PseudorandomNumberGenerator(HIGHEST_POSSIBLE_CASE_REF - LOWEST_POSSIBLE_CASE_REF)


def get_case_ref(sequence_number: int, case_ref_generator_key: bytes):
    pseudo_random_number = PSEUDORANDOM_NUMBER_GENERATOR.get_pseudorandom(sequence_number,
                                                                          case_ref_generator_key)

    case_ref_without_check_digit = pseudo_random_number + LOWEST_POSSIBLE_CASE_REF
    return add_check_digit(case_ref_without_check_digit)
