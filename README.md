# ssdc-rm-caseprocessor
Social (and Strategic) Survey Data Collection (SDC) Response Management (RM) Case Processor.

## Introduction
The case processor is responsible for managing a case, throughout its whole lifecycle: from sample load, to initial contact letter, to reminder letter, to the end of the collection exercise.

The case processor has the following data model schema structure:
```
  Survey
    └─ Collection Exercise
     ├─ Action Rule
     └─ Case
```

So, we have surveys, which have many collection exercises. A collection exercise has many action rules (e.g. initial contact letter, reminder letter, outbound telephone, face-to-face fieldwork interview). A collection exercise has many cases. Each case is created from a row in the sample file, and is usually an address.

The case processor schedules action rules to be triggered at a specific date & time.

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
Queue name: `supportTool.caseProcessor.sample`
Exchange: N/A
Routing key: N/A

Example message:

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
Topic: `event_survey-launched`

Example message: 
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
Topic name: `event_receipt`

Example message:
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
Topic: `event_refusal`

Example message:
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
Topic: `event_survey-launched`

Example message:
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

## Action Rule
The case processor triggers an action rule at its specified date & time. An action rule triggers once and only once. An action rule applies to all cases for a collection exercise, according to a classifier, which allows certain cases to be filtered out. Receipted, refused and invalid cases are always filtered out.

The SQL to create an action rule is as follows:

```sql
insert into casev3.action_rule
values ('f2af7113-eb93-4946-930d-3775e81a2666',    -- action rule ID
        'left(sample ->> ''REGION'',1) = ''W''',   -- classifier (e.g. only Wales region cases)
        'f',                                       -- always false, meaning not triggered
        'REMINDER_ONE',                            -- pack code
        'PPO',                                     -- print supplier
        '["ADDRESS_LINE1","TOWN_NAME","__uac__"]', -- CSV file template
        '20210528T08:00:00.000',                   -- date/time to trigger acton rule
        'PRINT',                                   -- type of action rule
        '0184cb41-0529-40ff-a2b7-08770249b95c');   -- collection exercise ID
```

The action rule will only be triggered by the "leader" pod, in the event that multiple instances of the case processor are running. If the leader dies or is killed/terminated, then another leader will be elected after 2 minutes.

## Fulfilment
The case processor triggers fulfilmments at a specified date & time. Fulfilments trigger once every 24 hours. Fulfilments will be sent for all requests which have queued up.

The SQL to create a print template is as follows:

```sql
insert into casev3.print_template
values ('TEST_FULFILMENT_CODE',      -- fulfilment code
        '["__caseref__","__uac__"]', -- print template
        'SUPPLIER_A');               -- print supplier
```

The SQL to create a fulfilment trigger is as follows:

```sql
insert into casev3.fulfilment_next_trigger
values ('b1d826fa-6afc-4270-9f0c-302cb05d4d96', -- trigger ID
        '2021-06-28T10:40:00.000Z');            -- next time to trigger
```

The fulfilments will only be triggered by the "leader" pod, in the event that multiple instances of the case processor are running. If the leader dies or is killed/terminated, then another leader will be elected after 2 minutes.

Topic: `event.paper-fulfilment`

Example message:
```json
{
  "event": {
    "type": "FULFILMENT",
    "source": "Respondent Home",
    "channel": "RH",
    "dateTime": null,
    "transactionId": null
  },
  "payload": {
    "fulfilment": {
      "caseId": "2a792cbe-c125-4a3b-973a-34c0bccc7656",
      "packCode": "TEST_FULFILMENT_CODE"
    }
  }
}
```

## Update Sensitive Data
If we want to change or blank out (i.e. 'delete') sensitive data, such as phone numbers, email addresses, or respondent names, to comply with GDPR right to rectification laws, then we can handle messages to fix that data as required.

Topic: `event.update-sample-sensitive`

Example message:
```json
{
  "event": {
    "type": "UPDATE_SAMPLE_SENSITIVE",
    "source": "RH",
    "channel": "RH",
    "dateTime": "2021-06-09T13:49:19.716761Z",
    "transactionId": "92df974c-f03e-4519-8d55-05e9c0ecea43"
  },
  "payload": {
    "updateSampleSensitive": {
      "caseId": "3b768940-6ef2-460e-bb75-e51d3a65ada4",
      "sampleSensitive": {"PHONE_NUMBER":"this is not a real phone number innit"}
    }
  }
}
```

## Deactivate UAC
Topic: `event_deactivate-uac`

Example message:
```json
{
  "event": {
    "type": "DEACTIVATE_UAC",
    "source": "RH",
    "channel": "RH",
    "dateTime": "2021-06-09T13:49:19.716761Z",
    "transactionId": "92df974c-f03e-4519-8d55-05e9c0ecea43"
  },
  "payload": {
    "deactivateUac": {
      "qid": "0123456789"
    }
  }
}
```


## Case Created
Topic: `event.case-update`

```json
{
  "event": {
    "type": "CASE_CREATED",
    "source": "CASE_PROCESSOR",
    "channel": "RM",
    "dateTime": "2021-08-04T11:51:00.000Z",
    "transactionId": "67c27976-dea9-438d-9785-0b58986dff4a"
  },
  "payload": {
      "collectionCase": {
        "caseId": "68f7b09b-3ca6-4c28-a5f3-08410f9ee579", 
        "receiptReceived": true,
        "invalidAddress": false,
        "surveyLaunched": true,
        "refusalReceived": "HARD_REFUSAL",
        "sample" : {
          "ADDRESS_LINE1": "123 Fake Street",
          "ADDRESS_LINE2": "JoVDP Village",
          "ADDRESS_LINE3": "Whilesville",
          "TOWN_NAME": "Mort City",
          "POSTCODE": "PO99 3XY"
        }
      }
  }
}
```

## Case Updated
Topic: `event.case-update`

```json
{
  "event": {
    "type": "CASE_UPDATED",
    "source": "CASE_PROCESSOR",
    "channel": "RM",
    "dateTime": "2021-08-04T11:51:00.000Z",
    "transactionId": "67c27976-dea9-438d-9785-0b58986dff4a"
  },
  "payload": {
      "collectionCase": {
        "caseId": "68f7b09b-3ca6-4c28-a5f3-08410f9ee579", 
        "receiptReceived": true,
        "invalidAddress": false,
        "surveyLaunched": true,
        "refusalReceived": "HARD_REFUSAL",
        "sample" : {
          "ADDRESS_LINE1": "123 Fake Street",
          "ADDRESS_LINE2": "JoVDP Village",
          "ADDRESS_LINE3": "Whilesville",
          "TOWN_NAME": "Mort City",
          "POSTCODE": "PO99 3XY"
        }
      }
  }
}
```

## UAC Updated
Topic: `event.uac-update`

```json
{
  "event": {
    "type": "UAC_UPDATED",
    "source": "CASE_PROCESSOR",
    "channel": "RM",
    "dateTime": "2021-08-04T11:51:00.000Z",
    "transactionId": "67c27976-dea9-438d-9785-0b58986dff4a"
  },
  "payload": {
    "uac": {
      "uac": "ABCD1234XYZA8765POYT",
      "active": true,
      "questionnaireId": "013000000000000164",
      "caseId": "68f7b09b-3ca6-4c28-a5f3-08410f9ee579",
      "collectionExerciseId": "58c12f89-ade1-412d-bbce-3d05916e4e64"
    }
  }
}
```