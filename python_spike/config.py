import os

# class Config:
#     PUBSUB_NEW_CASE_SUBSCRIPTION = os.getenv('PUBSUB_NEW_CASE_SUBSCRIPTION', 'event_new-case_rm-case-processor')
#     PUBSUB_EMULATOR_HOST = os.getenv('PUBSUB_EMULATOR_HOST', 'pubsub-emulator:8538')
#     PUBSUB_PROJECT = os.getenv('PUBSUB_PROJECT', 'our-project')


class PubsubConfig:
    PROJECT_ID = "our-project"

    DEFAULT_TIMEOUT = 30

    NEW_CASE_TOPIC_ID = "event_new-case"
    CASE_UPDATE_TOPIC = "event_case-update"

    SUBSCRIPTION_ID = "event_new-case_rm-case-processor"


class DatabaseConfig:
    DB_USERNAME = os.getenv('DB_USERNAME', 'appuser')
    DB_PASSWORD = os.getenv('DB_PASSWORD', 'postgres')
    DB_HOST = os.getenv('DB_HOST', 'localhost')
    DB_PORT = os.getenv('DB_PORT', '6432')
    DB_NAME = os.getenv('DB_NAME', 'rm')


class CaseRegGeneratorConfig:
    # This gives a caseRef 9 long, plus 1 checkbit at the end = 10
    LOWEST_POSSIBLE_CASE_REF = 100000000
    HIGHEST_POSSIBLE_CASE_REF = 999999999
