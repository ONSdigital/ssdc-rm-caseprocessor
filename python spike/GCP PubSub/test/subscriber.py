import json
from dataclass_wizard import fromdict
import pubsub
from dto.event_dto import EventDTO

timeout = 5.0


def callback(message) -> None:
    print(f"Received {message.data}")
    message.ack()


streaming_pull_future = pubsub.SUBSCRIBER.subscribe(pubsub.SUBSCRIPTION_PATH, callback=callback)
print(f"Listening for messages on {pubsub.SUBSCRIPTION_PATH()}")

with pubsub.SUBSCRIBER():
    try:
        streaming_pull_future.result(timeout=timeout)
    except TimeoutError:
        streaming_pull_future.cancel()
        streaming_pull_future.result()


def receive_new_case(message):
    # For maintainability could be moved to a separate file/class
    try:
        parsed_json = json.loads(message)
        event = fromdict(EventDTO, parsed_json)

        new_case_payload = event.payload.new_case

        #EventDTO(header=EventHeaderDTO(**parsed_json.pop('header')), payload=PayloadDTO(**parsed_json.pop('payload')))
    except TypeError:
        # TODO: throw error if json is wrong/can't cast to class
        print("something went wrong")