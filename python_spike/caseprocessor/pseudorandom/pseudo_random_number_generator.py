from .number_theory import factor
from .fpe_encryptor import FPEEncryptor
from config import CaseRegGeneratorConfig

"""
 Format Preserving 'Encryption' using the scheme FE1 from the paper "Format-Preserving Encryption"
 by Bellare, Rogaway, et al (http://eprint.iacr.org/2009/251).
 """

LOWEST_SAFE_NUMBER_OF_ROUNDS = 3

modulus = CaseRegGeneratorConfig.HIGHEST_POSSIBLE_CASE_REF - CaseRegGeneratorConfig.LOWEST_POSSIBLE_CASE_REF
first_factor, second_factor = factor(modulus)


def get_pseudorandom(original_number: int, key: bytes) -> int:
    """
    Generic Z_n FPE 'encryption' using the FE1 scheme.

    :param original_number: The number to 'encrypt'. Must not be null.
    :param key: Secret key to 'salt' the hashing with.
    :return: the encrypted version of <code>originalNumber</code>.
    :raises ValueError: if any of the parameters are invalid.
    """
    if original_number > modulus:
        raise ValueError("Cannot encrypt a number bigger than the modulus "
                         "(otherwise this wouldn't be format preserving encryption")

    encryptor = FPEEncryptor(key, modulus)

    pseudo_random_number = original_number

    # Apply the same algorithm repeatedly on x for the number of rounds given by getNumberOfRounds.
    # Each round increases the security.
    # Note that the attribute and method names used align to the paper on FE1, not Python conventions on readability
    for i in range(LOWEST_SAFE_NUMBER_OF_ROUNDS):
        # Split the value of x in to left and right values (think splitting the binary in to two halves),
        # around the second (smaller) factor
        left = int(pseudo_random_number / second_factor)
        right = int(pseudo_random_number % second_factor)

        w = int((left + encryptor.one_way_function(i, right)) % first_factor)
        pseudo_random_number = first_factor * right + w
    return pseudo_random_number
