#!/bin/sh

# Wait for pubsub-emulator to come up
bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' '$PUBSUB_SETUP_HOST')" != "200" ]]; do sleep 1; done'

# Below are the minimum RM topics & events - ideally external systems will publish/subscribe to these, in the correct standardised format

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/rm-internal-telephone-capture
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/rm-internal-telephone-capture.case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/rm-internal-telephone-capture"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/rm-internal-sample
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/rm-internal-sample.case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/rm-internal-sample"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.receipt
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.receipt.rm-case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.receipt"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.refusal
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.refusal.rm-case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.refusal"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/rm-internal-print-row
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/rm-internal-print-row.print-file-service -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/rm-internal-print-row"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.invalid
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.invalid.rm-case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.invalid"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.survey-launched
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.survey-launched.rm-case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.survey-launched"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.paper-fulfilment
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.paper-fulfilment.rm-case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.paper-fulfilment"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.deactivate-uac
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.deactivate-uac.rm-case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.deactivate-uac"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.update-sample-sensitive
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.update-sample-sensitive.rm-case-processor -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.update-sample-sensitive"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.case-update
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.case-update.rh -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.case-update"}'

curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/topics/event.uac-update
curl -X PUT http://$PUBSUB_SETUP_HOST/v1/projects/project/subscriptions/event.uac-update.rh -H 'Content-Type: application/json' -d '{"topic": "projects/project/topics/event.uac-update"}'

