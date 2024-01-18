import logging

from config import PubsubConfig
from .new_case_receiver import receive_new_case

timeout = 5.0


def callback(message) -> None:
    receive_new_case(message.data)
    message.ack()


def subscribe():
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
