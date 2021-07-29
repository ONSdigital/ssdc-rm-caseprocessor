#!/bin/sh

# Wait for pubsub-emulator to come up
bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' 'pubsub-emulator:8538')" != "200" ]]; do sleep 1; done'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/eq-submission-topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/rm-receipt-subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/eq-submission-topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/caseApi.caseProcessor.telephoneCapture.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/caseApi.caseProcessor.telephoneCapture.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/caseApi.caseProcessor.telephoneCapture.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/supportTool.caseProcessor.sample.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/supportTool.caseProcessor.sample.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/supportTool.caseProcessor.sample.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseProcessor.response.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseProcessor.response.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseProcessor.response.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseProcessor.refusal.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseProcessor.refusal.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseProcessor.refusal.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/caseProcessor.printFileSvc.printBatchRow.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/caseProcessor.printFileSvc.printBatchRow.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/caseProcessor.printFileSvc.printBatchRow.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseProcessor.invalidAddress.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseProcessor.invalidAddress.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseProcessor.invalidAddress.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseProcessor.surveyLaunched.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseProcessor.surveyLaunched.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseProcessor.surveyLaunched.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseProcessor.fulfilment.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseProcessor.fulfilment.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseProcessor.fulfilment.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseProcessor.deactivateUac.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseProcessor.deactivateUac.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseProcessor.deactivateUac.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseProcessor.updateSampleSensitive.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseProcessor.updateSampleSensitive.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseProcessor.updateSampleSensitive.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.caseUpdate.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.caseUpdate.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.caseUpdate.topic"}'

curl -X PUT http://pubsub-emulator:8538/v1/projects/project/topics/events.uacUpdate.topic
curl -X PUT http://pubsub-emulator:8538/v1/projects/project/subscriptions/events.uacUpdate.subscription -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/events.uacUpdate.topic"}'

