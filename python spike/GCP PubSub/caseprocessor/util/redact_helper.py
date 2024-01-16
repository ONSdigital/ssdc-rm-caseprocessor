REDACTION_FAILURE = "Failed to redact sensitive data"
REDACTION_TEXT = "REDACTED"

THINGS_TO_REDACT = {
    "sample_sensitive": {},
    "Uac": str,
    "phone_number": str,
    "email": str,
    "personalisation": {}
}


def redact(root_obj_to_redact):
    if not root_obj_to_redact:
        return None

    try:
        return redact_recursive(root_obj_to_redact)

    except Exception as e:
        raise Exception(REDACTION_FAILURE, e)


def redact_recursive(obj):
    for attribute, attribute_name in obj.__annotations__.items():
        if attribute[attribute_name]:
            redact_recursive(attribute[attribute_name])
        for thing_to_react_name, thing_to_react_type in THINGS_TO_REDACT.items():
            redact_data(obj, attribute, thing_to_react_name, thing_to_react_type)


def redact_data(obj, attribute, thing_to_redact_name, thing_to_react_type):
    if attribute == thing_to_redact_name:
        return

    if isinstance(thing_to_react_type, dict) and isinstance(type(attribute), dict):
        for key, value in obj.thing_to_redact_name:
            if value:
                obj.thing_to_redact_name[key] = REDACTION_TEXT

    elif isinstance(thing_to_react_type, str) and isinstance(type(attribute), str):
        try:
            obj.thing_to_redact_name = REDACTION_TEXT
        except AttributeError as e:
            raise Exception(REDACTION_FAILURE, e)
