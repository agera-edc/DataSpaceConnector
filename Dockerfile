# Dependecies
#FROM gradle:jdk11-alpine AS gradle
FROM eclipse-temurin:11-jdk-alpine AS runtime

WORKDIR /app

COPY ./ ./

RUN ["./gradlew", "samples:04.0-file-transfer:consumer:build"]
RUN ["./gradlew", "samples:04.0-file-transfer:provider:build"]

FROM runtime AS connector-provider

ENTRYPOINT ["java", "-Dedc.fs.config=samples/04.0-file-transfer/provider/config.properties", "-jar", "samples/04.0-file-transfer/provider/build/libs/provider.jar"]

FROM runtime AS connector-consumer

ENTRYPOINT ["java", "-Dedc.fs.config=samples/04.0-file-transfer/consumer/config.properties", "-jar", "samples/04.0-file-transfer/consumer/build/libs/consumer.jar"]
