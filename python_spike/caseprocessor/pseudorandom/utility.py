# Utility methods that swap between BigInteger, Java primitives and byte array representations of
#  numbers.


def convert_to_bytes_and_strip_leading_zero(n: int) -> bytes:
    if n == 0:
        return bytes('0', "utf-8")

    n_as_byte_array = to_bytes(n)

    return n_as_byte_array.lstrip(b'\x00')


def to_bytes(i: int) -> bytes:
    """
    Returns bytes representing the 32-bit integer passed in a 4 element byte in
    big-endian form.

    :param i: the integer to create the byte array for
    :return: the 4 element byte. Never null.
    """
    return int(i).to_bytes(4, byteorder="big")


def convert_final_value_into_positive_int(encrypted_value_bytes: bytes) -> int:
    return abs(int.from_bytes(encrypted_value_bytes, "big"))
