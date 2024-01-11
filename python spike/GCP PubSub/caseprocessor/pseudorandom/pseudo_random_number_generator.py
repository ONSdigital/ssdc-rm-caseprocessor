from .NumberTheory import NumberTheory
from .utility import convert_to_byte_array_and_strip_leading_zero, to_bytes
import io
from .hash_utility import digest


class PseudorandomNumberGenerator:
    wip: bytes
    LOWEST_SAFE_NUMBER_OF_ROUNDS = 3
    MAX_N_BYTES = 16

    def __init__(self, modulus):
        self.modulus = modulus
        self.factors = NumberTheory.factor(modulus)
        self.first_factor = self.factors[0]
        self.first_factor_bi = int(self.first_factor)
        self.second_factor = self.factors[1]

    def fpe_encryptor(self, key, modulus):
        encode_modules = convert_to_byte_array_and_strip_leading_zero(modulus)

        if len(encode_modules) > self.MAX_N_BYTES:
            raise Exception("Size of encoded n is too large for FPE encryption (was "
                            + encode_modules
                            + " bytes, max permitted "
                            + str(self.MAX_N_BYTES)
                            + ")"
                            )

        baos = io.BytesIO()

        try:
            baos.write(to_bytes(len(encode_modules)))
            baos.write(encode_modules)

            baos.write(to_bytes(len(key)))
            baos.write(key)

        except Exception as e:
            raise RuntimeError("Unable to write to byte array output stream!", e)

        self.wip = digest(bytes_to_digest=baos.getvalue())

    def get_pseudorandom(self, original_number, key):
        if original_number > self.modulus:
            raise ValueError("Cannot encrypt a number bigger than the modulus "
                             "(otherwise this wouldn't be format preserving encryption")

        self.fpe_encryptor(key, self.modulus)

        pseudo_random_number = original_number

        for i in range(self.LOWEST_SAFE_NUMBER_OF_ROUNDS):
            left = int(pseudo_random_number / self.second_factor)
            riight = pseudo_random_number % self.second_factor

            w = (left + self.__one_way_function(i - 1, riight)) % self.first_factor_bi
            pseudo_random_number = self.first_factor * riight + w

        return pseudo_random_number

    def __one_way_function(self, round_no, value_to_encrypt):
        r_bin = convert_to_byte_array_and_strip_leading_zero(value_to_encrypt)

        baos = io.BytesIO()

        try:
            baos.write(self.wip)
            baos.write(to_bytes(round_no))

            baos.write(to_bytes(len(r_bin)))
            baos.write(r_bin)

            digest_bytes = digest(baos.getvalue())
            return self.__turn_final_value_into_positive_big_int(digest_bytes)

        except Exception as e:
            raise RuntimeError("Unable to write to internal byte array,"
                               " this should never happen so indicates a defect in the code", e)

    @staticmethod
    def __turn_final_value_into_positive_big_int(encrypted_value_bytes):
        return abs(int(encrypted_value_bytes))
