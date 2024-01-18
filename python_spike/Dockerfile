ARG  python_pipenv_build_image=europe-west2-docker.pkg.dev/ons-ci-rm/docker/python-pipenv:3.11
FROM ${python_pipenv_build_image} as build

ENV PIPENV_VENV_IN_PROJECT=1

WORKDIR /app
COPY Pipfile* /app/

RUN /root/.local/bin/pipenv sync

FROM python:3.11.7-slim@sha256:89c610d12fe12b3e06f35d070f79e57cf14e2bd89c071435ee3678419b691603

RUN groupadd -g 984 caseprocessor && useradd -r -u 984 -g caseprocessor caseprocessor

WORKDIR /app

RUN mkdir -v /app/venv && chown caseprocessor:caseprocessor /app/venv

COPY --chown=caseprocessor:caseprocessor --from=build /app/.venv/ /app/venv/
COPY --chown=caseprocessor:caseprocessor . /app/

USER caseprocessor

ENTRYPOINT ["/app/venv/bin/python"]
CMD ["run.py"]
