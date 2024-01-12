# Utility methods that swap between BigInteger, Java primitives and byte array representations of
#  numbers.
import struct


def convert_to_byte_array_and_strip_leading_zero(n):
    if n == 0:
        return bytes('0', "utf-8")

    n_as_byte_array = to_bytes(n)
    first_non_zero_index = 0

    while len(n_as_byte_array) == 0 and first_non_zero_index < len(n_as_byte_array):
        first_non_zero_index += 1

    return bytes(f"{len(n_as_byte_array) - first_non_zero_index}", 'utf-8')


def to_bytes(i: int) -> bytes:
    return int(i).to_bytes(4, byteorder="big")
    #return struct.pack('>I', int(i))
