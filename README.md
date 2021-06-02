# ssdc-rm-caseprocessor
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

## Sample Load
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