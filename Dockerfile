FROM eclipse-temurin:17-jre

ARG JAR_FILE=ssdc-rm-caseprocessor*.jar
CMD ["/opt/java/openjdk/bin/java", "-jar", "/opt/ssdc-rm-caseprocessor.jar"]
COPY healthcheck.sh /opt/healthcheck.sh
RUN addgroup --gid 999 caseprocessor && \
    adduser --system --uid 999 caseprocessor
USER caseprocessor

COPY target/$JAR_FILE /opt/ssdc-rm-caseprocessor.jar
