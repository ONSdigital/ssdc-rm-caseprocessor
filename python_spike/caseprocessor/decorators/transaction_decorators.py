from functools import wraps
from typing import Callable
import logging
from db.db_utility import Session


'''
Example callback function signature:

@pubsub_transaction
@sqlalchemy_transaction
def callback(message: pubsub_v1.subscriber.message.Message, session: Session) -> None:
    ... 

'''
 
def pubsub_transaction(sql_transaction: Callable) -> Callable:
    @wraps(sql_transaction)
    def with_pubsub_error_handling(*args, **kwargs):
        pubsub_message = args[0]

        try:
            sql_transaction(*args, **kwargs)
            pubsub_message.ack()
            logging.info(f"Message acked: {pubsub_message.data}")

        except Exception as e:
            logging.error(f"PubSub decorator caugth error: {e}")
            pubsub_message.nack()
            logging.info(f"Message nacked: {pubsub_message.data}\n")

    return with_pubsub_error_handling


def sqlalchemy_transaction(callback: Callable) -> Callable:

    @wraps(callback)
    def with_transaction_handling(*args, **kwargs):
        local_session = Session()
        try:
            callback(*args, **kwargs, session=local_session)
            local_session.commit()
            Session.remove()
            logging.info("Session commited succesfully!")

        except Exception as e:
            Session.remove()
            logging.error(f"DB transaction decorator caught exception: {e}\n")
            raise Exception("SQL transaction error")

    return with_transaction_handling