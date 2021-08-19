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

So, we have surveys, which have many collection exercises. A collection exercise has many action rules (e.g. initial contact letter, reminder letter, outbound telephone, face-to-face fieldwork interview). A collection exercise has many cases. Each case is created from a row in the sample file.

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

## Running Locally

You can run this service dockerised with [docker dev](https://github.com/ONSdigital/ssdc-rm-docker-dev).

### Debugging
If you wish to run it from an IDE to debug first make sure you've set these environment variables in the run configuration so that it uses your local backing services, then spin up docker dev as usual and stop the `caseprocessor` container so it does not conflict.

```shell
SPRING_CLOUD_GCP_PUBSUB_EMULATOR_HOST=localhost:8538
```

## Internal Events

### Sample Load
Topic: `rm-internal-sample_case-processor`

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

### Enriched SMS Fulfilment
Topic: `rm-internal-sms-fulfilment_case-processor`

Example message:
```json
{
  "header": {
    "source": "TEST",
    "channel": "TEST",
    "topic": "rm-internal-sms-fulfilment_case-processor",
    "version": "v0.2_RELEASE",
    "dateTime": "2021-06-09T13:49:19.716761Z",
    "messageId": "92df974c-f03e-4519-8d55-05e9c0ecea43",
    "correlationId": "d6fc3b21-368e-43b5-baad-ef68d1a08629",
    "originatingUser": "dummy@example.com"
  },
  "payload": {
    "smsFulfilment": {
      "caseId": "2f2dc309-37cf-4749-85ea-ccb76ee69e4d",
      "packCode": "TEST_SMS_UAC",
      "uac": "RANDOMCHARS",
      "qid": "0123456789"
    }
  }
}
```

## Action Rules
The case processor triggers an action rule at its specified date & time. An action rule triggers once and only once. An action rule applies to all cases for a collection exercise, according to a classifier, which allows certain cases to be filtered out. Receipted, refused and invalid cases are always filtered out.

The SQL to create an action rule is as follows:

```sql
insert into casev3.action_rule
values ('f2af7113-eb93-4946-930d-3775e81a2666',    -- action rule ID
        'left(sample ->> ''REGION'',1) = ''W''',   -- classifier (e.g. only Wales region cases)
        'f',                                       -- always false, meaning not triggered
        'REMINDER_ONE',                            -- pack code
        'PPO',                                     -- print supplier
        '["BOAT_NAME","MARINA_BERTH","__uac__"]',  -- CSV file template
        '20210528T08:00:00.000',                   -- date/time to trigger acton rule
        'PRINT',                                   -- type of action rule
        '0184cb41-0529-40ff-a2b7-08770249b95c');   -- collection exercise ID
```

The action rule will only be triggered by the "leader" pod, in the event that multiple instances of the case processor are running. If the leader dies or is killed/terminated, then another leader will be elected after 2 minutes.
