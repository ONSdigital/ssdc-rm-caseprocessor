import dataclasses
from typing import Dict
from dataclasses import fields

REDACTION_FAILURE = "Failed to redact sensitive data"
REDACTION_TEXT = "REDACTED"

THINGS_TO_REDACT = {
    "sample_sensitive": dict,
    "Uac": str,
    "phone_number": str,
    "email": str,
    "personalisation": dict
}


def redact(root_obj_to_redact) -> object:
    if not root_obj_to_redact:
        return None

    try:
        redact_recursive(root_obj_to_redact)
        return root_obj_to_redact

    except Exception as e:
        raise Exception(REDACTION_FAILURE, e)


def redact_recursive(obj):
    for field in fields(obj):
        attribute = getattr(obj, field.name, None)
        if dataclasses.is_dataclass(attribute):
            redact_recursive(attribute)
        for thing_to_react_name, thing_to_react_type in THINGS_TO_REDACT.items():
            redact_data(obj, field.name, field.type, thing_to_react_name, thing_to_react_type)


def redact_data(obj, attribute, attribute_type, thing_to_redact_name, thing_to_react_type):
    if attribute != thing_to_redact_name:
        return

    # This could probably be made more efficient and could be condensed - since at the moment it duplicates a dict
    # by using dict comprehension or even assigning the key value straight away,
    # But I've left it as this for the spike since it's more readable
    if thing_to_react_type == dict and (attribute_type == dict or attribute_type == Dict[str, str]):
        redacted_dict = {}
        for key, value in getattr(obj, thing_to_redact_name).items():
            if not value:
                redacted_dict[key] = ''
            else:
                redacted_dict[key] = REDACTION_TEXT
        setattr(obj, attribute, redacted_dict)
    elif thing_to_react_type == str and attribute_type == str:
        try:
            setattr(obj, thing_to_redact_name, REDACTION_TEXT)
        except AttributeError as e:
            raise Exception(REDACTION_FAILURE, e)
