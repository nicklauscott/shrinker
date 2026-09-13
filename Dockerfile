# ---------- Stage 1: Build ----------
#FROM gradle:8.5-jdk21 AS builder
FROM gradle:9.7.1-jdk21 AS builder

WORKDIR /app

COPY gradle ./gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

#RUN ./gradlew dependencies --no-daemon
RUN gradle dependencies --no-daemon # remove later

COPY src ./src

#RUN ./gradlew clean bootJar --no-daemon
RUN gradle clean bootJar --no-daemon # remove later


# ---------- Stage 2: Run ----------
FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

RUN mkdir -p /app/Shrinker/db

EXPOSE 8001

ENTRYPOINT ["java", "-jar", "app.jar"]