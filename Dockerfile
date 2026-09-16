# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
# 의존성만 먼저 받아 레이어 캐시 - src만 바뀌면 이 레이어는 재사용된다
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --home-dir /app appuser

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
