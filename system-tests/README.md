# System tests

## Tracing integration tests

The [TracingIntegrationTests](./tests/src/test/java/org/eclipse/dataspaceconnector/system/tests/local/FileTransferIntegrationTest.java) makes sure that tracing works correctly when triggering a file transfer.
This test triggers a file transfer with the opentelemetry java agent attached. The default trace exporter is based is the OTLP exporter based on GRPC protocol. That's why an OtlpGrpcServer is started to collect the traces.
To be able to run the `TracingIntegrationTests` locally, you need to place the [opentelemetry java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.12.0/opentelemetry-javaagent.jar) in the project root folder.
Then you can run the tests:
```bash
./gradlew -p system-tests/tests test -DincludeTags="OpenTelemetryIntegrationTest"
```