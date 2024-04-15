import logging

from caseprocessor.messaging.new_case_receiver import __case_ref_generator_key
from caseprocessor.util.case_ref_generator import *

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    print(get_case_ref(1, __case_ref_generator_key))
