ARG MAVEN_IMAGE=docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21
ARG JAVA_RUNTIME_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:21-jre-alpine
ARG MAVEN_SKIP_TESTS=false

FROM ${MAVEN_IMAGE} AS build
ARG MAVEN_SKIP_TESTS
WORKDIR /workspace
COPY rayk-server/pom.xml ./pom.xml
COPY rayk-server/maven-settings.xml /usr/share/maven/ref/rayk-settings.xml
COPY rayk-server/src ./src
COPY database/migrations ./src/main/resources/db/migration
RUN --mount=type=cache,target=/root/.m2 mvn -s /usr/share/maven/ref/rayk-settings.xml -B -ntp -DskipTests=${MAVEN_SKIP_TESTS} clean package

FROM ${JAVA_RUNTIME_IMAGE}
ENV LANG=C.UTF-8 TZ=Asia/Shanghai JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
RUN addgroup -S rayk && adduser -S rayk -G rayk && mkdir -p /app/logs && chown -R rayk:rayk /app
WORKDIR /app
COPY --from=build --chown=rayk:rayk /workspace/target/rayk-server.jar /app/rayk-server.jar
USER rayk
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/rayk-server.jar"]
