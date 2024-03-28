import hashlib
import binascii


def generate_hash(string_to_hash: str = None, bytes_to_hash: bytes = None) -> str:
    if string_to_hash:
        bytes_to_hash = string_to_hash.encode('utf-8')

    hash_result = digest(bytes_to_hash)
    return binascii.hexlify(hash_result).decode('utf-8').lower()


def digest(bytes_to_digest: bytes) -> bytes:
    return hashlib.sha256(bytes_to_digest).digest()
