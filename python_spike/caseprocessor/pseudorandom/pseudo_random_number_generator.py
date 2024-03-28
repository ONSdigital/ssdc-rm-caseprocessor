import logging

from .NumberTheory import factor
from .fpe_encryptor import FPEEncryptor


class PseudorandomNumberGenerator:
    """
     Format Preserving 'Encryption' using the scheme FE1 from the paper "Format-Preserving Encryption"
     by Bellare, Rogaway, et al (http://eprint.iacr.org/2009/251).
     """

    LOWEST_SAFE_NUMBER_OF_ROUNDS = 3

    def __init__(self, modulus):
        self.modulus = modulus
        self.factors = factor(modulus)
        self.first_factor = self.factors[0]
        self.second_factor = self.factors[1]

    def get_pseudorandom(self, original_number: int, key: bytes) -> int:
        """
        Generic Z_n FPE 'encryption' using the FE1 scheme.

        :param original_number: The number to 'encrypt'. Must not be null.
        :param key: Secret key to 'salt' the hashing with.
        :return: the encrypted version of <code>originalNumber</code>.
        :raises ValueError: if any of the parameters are invalid.
        """
        if original_number > self.modulus:
            raise ValueError("Cannot encrypt a number bigger than the modulus "
                             "(otherwise this wouldn't be format preserving encryption")

        encryptor = FPEEncryptor(key, self.modulus)

        pseudo_random_number = original_number

        # Apply the same algorithm repeatedly on x for the number of rounds given by getNumberOfRounds.
        # Each round increases the security.
        # Note that the attribute and method names used align to the paper on FE1, not Python conventions on readability
        for i in range(self.LOWEST_SAFE_NUMBER_OF_ROUNDS):
            # Split the value of x in to left and right values (think splitting the binary in to two halves),
            # around the second (smaller) factor
            left = int(pseudo_random_number / self.second_factor)
            right = int(pseudo_random_number % self.second_factor)

            print(f"Left = {left}   Right = {right}")

            w = int((left + encryptor.one_way_function(i, right)) % self.first_factor)
            pseudo_random_number = self.first_factor * right + w
        return pseudo_random_number
