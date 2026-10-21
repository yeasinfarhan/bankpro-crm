# BankPro backend image (used by Render and by `docker compose --profile app`).
# Stage 1 builds the jar with Maven; stage 2 runs it on a small Java 21 runtime as a non-root user.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run in CI / on your PC (they need MySQL); the image build only compiles and packages.
RUN mvn -B -q -Dmaven.test.skip=true package && cp target/bankpro-backend-*.jar app.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system bankpro && useradd --system --gid bankpro --uid 10001 bankpro
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER bankpro
# Sized for a 512 MB container. Measured after a full end-to-end run: ~60 MB live heap, ~110 MB class
# metadata, ~45 MB compiled code. Heap is capped at half the memory so the rest always fits.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod \
    PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
