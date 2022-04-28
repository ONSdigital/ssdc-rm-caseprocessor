FROM eclipse-temurin:17.0.3_7-jdk

ARG JAR_FILE=ssdc-rm-caseprocessor*.jar

CMD ["java", "-jar", "/opt/ssdc-rm-caseprocessor.jar"]
COPY healthcheck.sh /opt/healthcheck.sh
RUN groupadd --gid 999 caseprocessor && \
    useradd --create-home --system --uid 999 --gid caseprocessor caseprocessor
USER caseprocessor

COPY target/$JAR_FILE /opt/ssdc-rm-caseprocessor.jar