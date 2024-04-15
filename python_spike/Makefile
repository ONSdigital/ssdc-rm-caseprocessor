install:
	pipenv install --dev

run:
	PUBSUB_EMULATOR_HOST=localhost:8538 pipenv run python run.py

test:
	pipenv run python run_tests.py

docker_build:
	docker build -t europe-west2-docker.pkg.dev/ssdc-rm-ci/docker/caseprocessorpy .