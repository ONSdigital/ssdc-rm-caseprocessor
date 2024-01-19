import logging
import time

from config import PubsubConfig
from .new_case_receiver import receive_new_case

timeout = 5.0


def callback(message) -> None:
    receive_new_case(message.data)
    message.ack()


def subscribe():
    streaming_pull_future = PubsubConfig.SUBSCRIBER.subscribe(PubsubConfig.SUBSCRIPTION_PATH, callback=callback)
    logging.info(f"Listening for messages on {PubsubConfig.SUBSCRIPTION_PATH}")

    wait_till_pubsub_ready()

    with PubsubConfig.SUBSCRIBER:
        try:
            streaming_pull_future.result()
        except TimeoutError:
            print("closing")
            streaming_pull_future.cancel()
            streaming_pull_future.result()
        except KeyboardInterrupt:
            print("----Manually Stopped Subscriber----")


# Keeps trying to check if there is a subscription
# I know this isn't very nice - I also don't like it but...
# This is just temporary for the spike,
# without it sometimes the container crashes as the pubsub docker isn't ready
def wait_till_pubsub_ready():
    tries = 0
    while tries < 10:
        try:
            PubsubConfig.SUBSCRIBER.get_subcription(PubsubConfig.SUBSCRIPTION_PATH)
            return
        except:
            tries += 1
            time.sleep(5)