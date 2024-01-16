import celery
import celery_pubsub


# @celery_pubsub.subscribe_to('event-new-case') - This could be helpful, but it's in a pre-release
@celery.shared_task
def new_case(*args, **kwargs):
    print(args, kwargs)
    return "task 1 done"


# First, let's subscribe
celery_pubsub.subscribe('test-topic', new_case)
