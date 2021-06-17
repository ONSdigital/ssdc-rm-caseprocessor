# ssdc-rm-caseprocessor
TEST
Social (and Strategic) Survey Data Collection (SDC) Response Management (RM) Case Processor.

## Introduction
The case processor is responsible for managing a case, throughout its whole lifecycle: from sample load, to initial contact letter, to reminder letter, to the end of the collection exercise.

The case processor has the following data model schema structure:
```
  Survey
    └─ Collection Exercise
     ├─ Wave of Contact
     └─ Case
```

So, we have surveys, which have many collection exercises. A collection exercise has many waves of contact (e.g. initial contact letter, reminder letter, outbound telephone, face-to-face fieldwork interview). A collection exercise has many cases. Each case is created from a row in the sample file, and is usually an address.

The case processor schedules waves of contact to be triggered at a specific date & time.

The case processor is responsible for creating the content of the CSV print file which will be used to print letters, according to a flexible template. The print files can contain case refs, UACs, QIDs and any attribute of the sample.

## Building
To run all the tests and build the image
```  
   mvn clean install
```

Just build the image
```
    mvn -DskipTests -DskipITs -DdockerCompose.skip
```

## Events

### Sample Load
The case processor listens to a Rabbit queue called `case.sample.inbound`. The messages are JSON format and are of the following format:

```json
{
  "caseId": "b3d63a55-67bd-4f38-a18d-41297d699d5d",
  "collectionExerciseId": "58d971eb-c8f0-45b4-bfd8-af7b9ad8c781",
  "sample": {
    "foo": "bar"
  }
}
```

### Invalid Address
Queue Name: case.invalidaddress
Example Msg: 
```json
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
```

### Receipts/Responses
Queue Name: Case.Responses
Example Msg:
```json
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
```

### Refusals
Queue: case.refusals
Example Msg:
```json
{
  "event": {
    "type": "REFUSAL_RECEIVED",
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
```

### Survey Launched
queue: survey.launched
Example Msg:
```json
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
```


Each sample message will create a case, linked to the specified collection exercise. The ID must be unique.

## Wave of Contact
The case processor triggers a wave of contact at its specified date & time. A wave of contact triggers once and only once. A wave of contact applies to all cases for a collection exercise, according to a classifier, which allows certain cases to be filtered out. Receipted, refused and invalid cases are always filtered out.

The SQL to create a wave of contact is as follows:

```sql
insert into casev3.wave_of_contact
values ('f2af7113-eb93-4946-930d-3775e81a2666',    -- wave of contact ID
        'left(sample ->> ''REGION'',1) = ''W''',   -- classifier (e.g. only Wales region cases)
        'f',                                       -- always false, meaning not triggered
        'REMINDER_ONE',                            -- pack code
        'PPO',                                     -- print supplier
        '["ADDRESS_LINE1","TOWN_NAME","__uac__"]', -- CSV file template
        '20210528T08:00:00.000',                   -- date/time to trigger wave of contact
        'PRINT',                                   -- type of wave of contact
        '0184cb41-0529-40ff-a2b7-08770249b95c');   -- collection exercise ID
```

The wave of contact will only be triggered by the "leader" pod, in the event that multiple instances of the case processor are running. If the leader dies or is killed/terminated, then another leader will be elected after 2 minutes.
