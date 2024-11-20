import logging
import time
from google.cloud import pubsub_v1

from config import PubsubConfig
from caseprocessor.messaging.new_case_receiver import receive_new_case

timeout = 5.0

subscriber = pubsub_v1.SubscriberClient()


def callback(message) -> None:
    receive_new_case(message.data)
    message.ack()


def subscribe():
    new_case_subscription = add_subscription_path(PubsubConfig.SUBSCRIPTION_ID)
    streaming_pull_future = subscriber.subscribe(new_case_subscription, callback=callback)

    wait_till_pubsub_ready()
    logging.info(f"Listening for messages on {new_case_subscription}")

    with subscriber:
        try:
            streaming_pull_future.result()
        except TimeoutError:
            print("closing")
            streaming_pull_future.cancel()
            streaming_pull_future.result()
        except KeyboardInterrupt:
            print("----Manually Stopped Subscriber----")


def add_subscription_path(subscription_id):
    return subscriber.subscription_path(PubsubConfig.PROJECT_ID, subscription_id)


# Keeps trying to check if there is a subscription
# I know this isn't very nice - I also don't like it but...
# This is just temporary for the spike,
# without it sometimes the container crashes as the pubsub docker isn't ready
def wait_till_pubsub_ready():
    tries = 0
    while tries < 10:
        try:
            subscriber.get_subcription(PubsubConfig.SUBSCRIPTION_PATH)
            return
        except:
            tries += 1
            time.sleep(5)
