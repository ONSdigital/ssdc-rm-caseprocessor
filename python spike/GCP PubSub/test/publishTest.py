from pubsub import PubsubConfig
import json
import uuid
from google.cloud import pubsub_v1

def generate_message():
    case_id = str(uuid.uuid4())
    message_id = str(uuid.uuid4())
    correlation_id = str(uuid.uuid4())
    collex_id = str(uuid.uuid4())
    originating_user = "foo.bar@ons.gov.uk"
    return json.dumps(
            {
                "header": {
                    "version": "0.5.0",
                    "topic": PubsubConfig.TOPIC_ID,
                    "source": "cupidatat",
                    "channel": "EQ",
                    "dateTime": "1970-01-01T00:00:00.000Z",
                    "messageId": message_id,
                    "correlationId": correlation_id,
                    "originatingUser": originating_user
                },
                "payload": {
                    "newCase": {
                        "caseId": case_id,
                        "collectionExerciseId": collex_id,
                        "sample": {
                            "schoolId": "abc123",
                            "schoolName": "Chesterthorps High School",
                        },
                        "sampleSensitive": {
                            "firstName": "Fred",
                            "lastName": "Bloggs",
                            "childFirstName": "Jo",
                            "childMiddleNames": "Rose May",
                            "childLastName": "Pinker",
                            "childDob": "2001-12-31",
                            "mobileNumber": "07123456789",
                            "emailAddress": "fred.bloggs@domain.com",
                            "consentGivenTest": "true",
                            "consentGivenSurvey": "true"
                        }
                    }
                }
            }
        )


future = PubsubConfig.PUBLISHER.publish(PubsubConfig.TOPIC_PATH, generate_message().encode('utf-8'))
print("publishing")
future.result()