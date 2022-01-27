# Dependecies
FROM gradle:jdk11-alpine AS gradle

WORKDIR /app

COPY ./* .

RUN ./gradlew samples:04.0-file-transfer:consumer:build
RUN ./gradlew samples:04.0-file-transfer:provider:build


# FROM eclipse-temurin:11-jre-alpine AS runtime

FROM gradle AS connector-provider

ENTRYPOINT ["java" ,"-Dedc.fs.config=samples/04.0-file-transfer/provider/config.properties", "-jar", "samples/04.0-file-transfer/provider/build/libs/provider.jar"]

FROM gradle AS connector-consumer

ENTRYPOINT ["java" ,"-Dedc.fs.config=samples/04.0-file-transfer/consumer/config.properties", "-jar", "samples/04.0-file-transfer/consumer/build/libs/consumer.jar"]
