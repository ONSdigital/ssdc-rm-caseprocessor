from google.cloud import pubsub_v1

class PubsubConfig:
    PROJECT_ID = "ssdc-rm-danielbanks02"

    PUBLISHER = pubsub_v1.PublisherClient()
    TOPIC_ID = "event_new-case"
    TOPIC_PATH = PUBLISHER.topic_path(PROJECT_ID, TOPIC_ID)

    SUBSCRIBER = pubsub_v1.SubscriberClient()
    SUBSCRIPTION_ID = "event_new-case_rm-case-processor"
    SUBSCRIPTION_PATH = SUBSCRIBER.subscription_path(PROJECT_ID, SUBSCRIPTION_ID)


"""
class PubsubInstance:

    def __init__(self):
        self.__project_id = "ssdc-rm-danielbanks02"
        
        # List of topic ids that the pubsub publishes to
        self.__topics = ["event-new-case"]
        # List of subscription ids that the pubsub is subscribed to
        self.__subscriptions = ["event_new-case_rm-case-processor"]

        self.__publisher = pubsub_v1.PublisherClient()
        for topic_id in self.__topics:
            self.__topic_path = self.__publisher.topic_path(self.__project_id, topic_id)

        self.__subscriber = pubsub_v1.SubscriberClient()
        for subscription_id in self.__subscriptions:
            self.__subscription_path = self.__subscriber.subscription_path(self.__project_id, subscription_id)
        
    def get_publisher(self):
        return self.__publisher

    def get_topic_path(self):
        return self.__topic_path

    def get_subscriber(self):
        return self.__subscriber

    def get_subscription_path(self):
        return self.__subscription_path
"""