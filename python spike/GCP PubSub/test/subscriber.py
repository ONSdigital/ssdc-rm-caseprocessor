import json
from dataclass_wizard import fromdict
from pubsub import PubsubConfig
from dto.event_dto import EventDTO

timeout = 5.0

def receive_new_case(message):
    # For maintainability could be moved to a separate file/class
    try:
        parsed_json = json.loads(message)
        event = fromdict(EventDTO, parsed_json)

        new_case_payload = event.payload.new_case

        print("----Json Deserialized----")
        print(new_case_payload)

    except TypeError as e:
        # TODO: throw error if json is wrong/can't cast to class
        print(f"something went wrong:\n{e}")

def callback(message) -> None:
    print(f"Received {message.data}")
    receive_new_case(message.data)
    message.ack()


streaming_pull_future = PubsubConfig.SUBSCRIBER.subscribe(PubsubConfig.SUBSCRIPTION_PATH, callback=callback)
print(f"Listening for messages on {PubsubConfig.SUBSCRIPTION_PATH}")

with PubsubConfig.SUBSCRIBER:
    try:
        streaming_pull_future.result()
    except TimeoutError:
        print("closing")
        streaming_pull_future.cancel()
        streaming_pull_future.result()
    except KeyboardInterrupt:
        print("----Manually Stopped Subscriber----")
