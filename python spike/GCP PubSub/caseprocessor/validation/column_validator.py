"""
This is just temporary for the spike - since common validation is out of scope

This will get replaced if and when we change common validation to python
"""
from typing import Optional, List
from .rule import Rule
from dataclasses import dataclass


# wasn't sure weather to make this a data class or not - decided to so it could be easier to deseiralise json

@dataclass
class ColumnValidator:
    column_name: str
    sensitive: str
    rules: List[Rule]

    """
    def __init__(self, column_name: str, sensitive: str, rules: List[Rule]):
        # I set these as private like in the java repo
        self.__column_name = column_name
        self.__sensitive = sensitive
        self.__rules = rules
    """

    def validate_data(self, data_to_validate, exclude_data_from_returned_error_msgs):
        validation_errors = []

        for rule in self.rules:
            validation_error = rule.check_validity(data_to_validate)

            if validation_error is not None:
                if exclude_data_from_returned_error_msgs:
                    validation_errors.append(
                        "Column '"
                        + self.column_name
                        + "' Failed validation for Rule '"
                        + rule.__name__
                        + "' validation error: "
                        + validation_error
                    )
                else:
                    validation_errors.append(
                        "Column '"
                        + self.column_name
                        + "' value '"
                        + data_to_validate
                        + "' validation error: "
                        + validation_error
                    )

        if not validation_errors:
            return ", ".join(validation_errors)

        return None

    def validate_row(self, row_data, exclude_data_from_returned_error_msgs=False):
        return self.validate_data(row_data[self.column_name], exclude_data_from_returned_error_msgs)

    def get_column_name(self):
        return self.column_name

    def is_sensitive(self):
        return self.sensitive

    def get_rules(self):
        return self.rules
