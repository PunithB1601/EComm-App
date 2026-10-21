# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Optional settings/truststore secrets support corporate build proxies without baking credentials into layers.
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=java_truststore,target=/tmp/java-truststore \
    if [ -f /tmp/java-truststore ]; then \
      mvn -B -Djavax.net.ssl.trustStore=/tmp/java-truststore -DskipTests package; \
    else mvn -B -DskipTests package; fi

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --gid 1000 identity && useradd --uid 1000 --gid identity --no-create-home identity
COPY --from=build --chown=identity:identity /build/target/identity-service-*.jar /app/identity-service.jar
USER identity
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/identity-service.jar"]
