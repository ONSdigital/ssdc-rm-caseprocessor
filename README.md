# ssdc-rm-caseprocessor
Social/Strategic Survey Data Collection Response Management (RM) Case Processor FTW

## What is it?
Super seriously strategic new RM (version 3.0) which can cope with flexible samples (i.e. any shape, business or social) and fulfils all of SDC's RM needs most excellently.



## Events

#Invalid Address
Queue Name: case.invalidaddress
Example Msg: 
{
  "event": {
    "type": "ADDRESS_NOT_VALID",
    "source": "RH",
    "channel": "RH",
    "dateTime": "2021-06-09T13:49:19.716761Z",
    "transactionId": "92df974c-f03e-4519-8d55-05e9c0ecea43"
  },
  "payload": {
    "invalidAddress": {
      "reason": "Not found",
      "notes": "Looked hard",
      "caseId": "3b768940-6ef2-460e-bb75-e51d3a65ada4"
    }
  }
}

#Receipts/Responses
Queue Name: Case.Responses
Example Msg:
{
  "event": {
    "type": "RESPONSE_RECEIVED",
    "source": "RH",
    "channel": "RH",
    "dateTime": "2021-06-09T14:10:11.910719Z",
    "transactionId": "730af73e-398d-41d2-893a-cd0722151f9c"
  },
  "payload": {
    "response": {
      "questionnaireId": "123456",
      "dateTime": "2021-06-09T14:10:11.909472Z"
    }
  }
}

#Refusals
Queue: case.refusals
Example Msg:
{
  "event": {
    "type": "RESPONSE_RECEIVED",
    "source": null,
    "channel": null,
    "dateTime": null,
    "transactionId": null
  },
  "payload": {
    "refusal": {
      "type": "EXTRAORDINARY_REFUSAL",
      "collectionCase": {
        "caseId": "2a792cbe-c125-4a3b-973a-34c0bccc7656",
        "receiptReceived": null,
        "invalidAddrress": null,
        "surveyLaunched": null,
        "refusalReceived": null,
        "sample": null
      }
    }
  }
}

#Survey Launched
queue: survey.launched
Example Msg:
{
  "event": {
    "type": "SURVEY_LAUNCHED",
    "source": "Respondent Home",
    "channel": "RH",
    "dateTime": null,
    "transactionId": null
  },
  "payload": {
    "response": {
      "questionnaireId": "1234334"
    }
  }
}

#Sample 
Queue Name: case.sample.inbound
Example Msg:
{
  "caseId": "37ee4eec-901a-4305-a2da-010e140d47c5",
  "collectionExerciseId": "ea830542-ff82-49fc-ad5a-9dc0a0eebe8e",
  "sample": {
    "Address": "Tenby",
    "Org": "Brewery"
  }
}


 
