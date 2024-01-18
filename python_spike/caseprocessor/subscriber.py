import logging
import time

from config import PubsubConfig
from .new_case_receiver import receive_new_case

timeout = 5.0


def callback(message) -> None:
    receive_new_case(message.data)
    message.ack()


def subscribe():
    # waiting 5 seconds before connecting
    # I know this isn't very nice - I also don't like it but...
    # This is just temporary for the spike, without it sometimes it fails as the postgres database or pubsub docker isn't ready
    time.sleep(10)

    streaming_pull_future = PubsubConfig.SUBSCRIBER.subscribe(PubsubConfig.SUBSCRIPTION_PATH, callback=callback)
    logging.info(f"Listening for messages on {PubsubConfig.SUBSCRIPTION_PATH}")

    with PubsubConfig.SUBSCRIBER:
        try:
            streaming_pull_future.result()
        except TimeoutError:
            print("closing")
            streaming_pull_future.cancel()
            streaming_pull_future.result()
        except KeyboardInterrupt:
            print("----Manually Stopped Subscriber----")
