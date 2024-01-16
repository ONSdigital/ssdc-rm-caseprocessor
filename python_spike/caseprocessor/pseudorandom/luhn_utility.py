import luhn


def add_check_digit(number):
    number_string = str(number)
    check_digit = luhn.generate(number_string)
    return f"{number_string}{check_digit}"
