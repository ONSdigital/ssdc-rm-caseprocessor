from google.cloud import pubsub_v1
from config import PubsubConfig

publisher = pubsub_v1.PublisherClient()


def publish_to_pubsub(topic, message):
    topic_path = publisher.topic_path(PubsubConfig.PROJECT_ID, topic)

    return publisher.publish(topic_path, message.encode('utf-8')).result(timeout=PubsubConfig.DEFAULT_TIMEOUT)

