from .primes import PRIMES


def count_low_zero_bits(n: int) -> int:
    """
    Return the number of 0 bits at the end of a binary representation of n.

    :parameter n: the number to examine
    :return: the number of 0's at the end of a binary representation of n.
    """

    low_zero = 0
    while n & 1 == 0 and n != 0:
        low_zero += 1
        n >>= 1
    return low_zero


def factor(number):
    """
    Factor num into a and b which are as close together as possible. Assumes n is composed mostly
    of small factors which is the case for typical uses of FPE (typically, n is a power of 10) Want
    a &gt;= b since the safe number of rounds is 2+log_a(b); if a &gt;= b then this is always 3

    :param number: the number to factor. This is typically the modulus. This must NOT be a prime
    number or an exception will be thrown.
    :return: a pair (always 2) factors of the num passed in which are as close to each other as
    possible.
    """

    n = number
    a = 1
    b = 1

    n_low_zero = count_low_zero_bits(n)

    a = a << int(n_low_zero / 2)
    b = b << int(n_low_zero - (n_low_zero / 2))
    n = n >> int(n_low_zero)

    for i in range(len(PRIMES)):
        while n % PRIMES[i] == 0:
            a = a * (PRIMES[i])

            if a > b:
                a, b = b, a
            n = n / (PRIMES[i])

    if a > b:
        a, b = b, a
    a = a * n

    if a < b:
        a, b = b, a

    # if (a <= 1 || b <= 1) then no factors exist, i.e. you've passed a prime number
    if a <= 1 or b <= 1:
        raise Exception(
            "Could not factor passed number for use in FPE.  "
            "This is usually caused by passing a prime number for a modulus.")

    return [a, b]
