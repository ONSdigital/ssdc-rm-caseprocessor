import os

# class Config:
#     PUBSUB_NEW_CASE_SUBSCRIPTION = os.getenv('PUBSUB_NEW_CASE_SUBSCRIPTION', 'event_new-case_rm-case-processor')
#     PUBSUB_EMULATOR_HOST = os.getenv('PUBSUB_EMULATOR_HOST', 'pubsub-emulator:8538')
#     PUBSUB_PROJECT = os.getenv('PUBSUB_PROJECT', 'our-project')

from google.cloud import pubsub_v1


class PubsubConfig:
    PROJECT_ID = "our-project"

    PUBLISHER = pubsub_v1.PublisherClient()
    TOPIC_ID = "event_new-case"
    TOPIC_PATH = PUBLISHER.topic_path(PROJECT_ID, TOPIC_ID)

    SUBSCRIBER = pubsub_v1.SubscriberClient()
    SUBSCRIPTION_ID = "event_new-case_rm-case-processor"
    SUBSCRIPTION_PATH = SUBSCRIBER.subscription_path(PROJECT_ID, SUBSCRIPTION_ID)

    CASE_UPDATE_TOPIC = "event_case-update"
    CASE_UPDATE_PATH = PUBLISHER.topic_path(PROJECT_ID, CASE_UPDATE_TOPIC)


class DatabaseConfig:
    DB_USERNAME = os.getenv('DB_USERNAME', 'appuser')
    DB_PASSWORD = os.getenv('DB_PASSWORD', 'postgres')
    DB_HOST = os.getenv('DB_HOST', 'localhost')
    DB_PORT = os.getenv('DB_PORT', '6432')
    DB_NAME = os.getenv('DB_NAME', 'rm')
