from enum import Enum


class RefusalType(Enum):
    HARD_REFUSAL = 1
    EXTRAORDINARY_REFUSAL = 2
    SOFT_REFUSAL = 3
    WITHDRAWAL_REFUSAL = 4
