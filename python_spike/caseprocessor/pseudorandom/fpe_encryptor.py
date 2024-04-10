import io
from .utility import convert_to_bytes_and_strip_leading_zero, to_bytes, convert_final_value_into_positive_int
from .hash_utility import digest


# A simple round function based on SHA-256
class FPEEncryptor:
    MAX_N_BYTES = 128 / 8  # Normally FPE is for SSNs, CC#s, etc.; so limit modulus to 128-bit numbers

    def __init__(self, key: bytes, modulus):
        """
        Initialise a new 'encryptor'.

        <p>Ultimately this initialises {@link #wip} by creating a byte array with:
         <ol>
            <li>Writing the length of the encoded value of the modulusBI.
            <li>Writing the encoded value of the modulusBI.
            <li>Writing the length of the key.
            <li>Writing the key.
         </ol>
         <p>And then setting {@link #wip} to the SHA256'd value of this byte array.

         :param key: the key to 'salt' the SHA256 digest.
         :param modulus: the range of the output numbers.
        """
        encode_modules = convert_to_bytes_and_strip_leading_zero(modulus)

        if len(encode_modules) > self.MAX_N_BYTES:
            raise ValueError("Size of encoded n is too large for FPE encryption (was "
                            + encode_modules
                            + " bytes, max permitted "
                            + str(self.MAX_N_BYTES)
                            + ")"
                            )

        bio = io.BytesIO()
        try:
            bio.write(to_bytes(len(encode_modules)))
            bio.write(encode_modules)
            bio.write(to_bytes(len(key)))
            bio.write(key)
        except OSError as e:
            raise RuntimeError("Unable to write to byte IO!", e)
        self.wip = digest(bio.getvalue())

    def one_way_function(self, round_no: int, value_to_encrypt: int) -> int:
        """
        Mixes the round number, the input value r and the previously calculated {@link #wip} in to a
        new value. Calling this repeatedly on the value of r with a new round number applies a
        one-way function to the value.

        <p>Works as follows:

        <ol>
            <li>Serialise r to minimal bytes with no leading zero bytes
            <li>Create bytes consisting of:
                <ol type="i">
                    <li>macNT value
                    <li>the round number
                    <li>the length of the serialised value of r (32-bit)
                    <li>the value of serialised r
                </ol>
            <li>Create an SHA256 value of the output array and convert back to an int, this is
                the encrypted r.
        </ol>

         :param round_no: to ensure that value is changed in a different way each time, increase this
                for each time you call the method on the same value.
         :param value_to_encrypt: the number that we are using as input to the function.
         :return: a new int value that has reversibly encrypted r.
        """
        r_bin = convert_to_bytes_and_strip_leading_zero(value_to_encrypt)
        bio = io.BytesIO()

        try:
            bio.write(self.wip)
            bio.write(to_bytes(round_no))

            bio.write(to_bytes(len(r_bin)))
            bio.write(r_bin)
            digest_bytes = digest(bio.getvalue())
            return convert_final_value_into_positive_int(digest_bytes)
        except OSError as e:
            raise RuntimeError("Unable to write to internal byte array,"
                               " this should never happen so indicates a defect in the code", e)
