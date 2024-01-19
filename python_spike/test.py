from config import PubsubConfig
import uuid
import json
from python_spike.caseprocessor.messaging.new_case_receiver import receive_new_case


def generate_message():
    case_id = str(uuid.uuid4())
    message_id = str(uuid.uuid4())
    correlation_id = str(uuid.uuid4())
    collex_id = "068de8bd-ea80-4442-86a6-705c14eb5ee7"
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


# printing the schema and tables in the database
#test_database()

receive_new_case(generate_message().encode('utf-8'))

