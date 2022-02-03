FROM openjdk:11-jre-slim AS runtime

WORKDIR /build

COPY ./consumer/build/libs/consumer.jar /build/consumer/
RUN echo "web.http.port=9191\n" \
         "edc.api.control.auth.apikey.value=password\n" \
         "ids.webhook.address=http://sample04-connector-consumer:9191\n" > /build/consumer/config.properties

COPY ./provider/build/libs/provider.jar /build/provider/
RUN echo "edc.samples.04.asset.path=/tmp/provider/test-document.txt\n" \
         "ids.webhook.address=http://sample04-connector-provider:8181\n" > /build/provider/config.properties


FROM runtime AS sample04-connector-provider

WORKDIR /app
COPY --from=runtime /build/provider/* /app/

ENTRYPOINT ["java", "-Dedc.fs.config=config.properties", "-jar", "provider.jar"]

FROM runtime AS sample04-connector-consumer

WORKDIR /app
COPY --from=runtime /build/consumer/* /app/

ENTRYPOINT ["java", "-Dedc.fs.config=config.properties", "-jar", "consumer.jar"]