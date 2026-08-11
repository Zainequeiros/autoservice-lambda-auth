import re


def only_digits(value: str) -> str:
    return re.sub(r"\D", "", value or "")


def is_valid_cpf(cpf: str) -> bool:
    digits = only_digits(cpf)
    if len(digits) != 11 or digits == digits[0] * 11:
        return False

    first_sum = sum(int(digits[i]) * (10 - i) for i in range(9))
    first_check = (first_sum * 10 % 11) % 10
    if first_check != int(digits[9]):
        return False

    second_sum = sum(int(digits[i]) * (11 - i) for i in range(10))
    second_check = (second_sum * 10 % 11) % 10
    return second_check == int(digits[10])
