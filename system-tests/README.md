# System tests

## Tracing integration tests

The [TracingIntegrationTests](./tests/src/test/java/org/eclipse/dataspaceconnector/system/tests/local/FileTransferIntegrationTest.java) make sure that tracing work correctly when triggering a file transfer.
To be able to run the `TracingIntegrationTests` locally, you need to place the [opentelemetry java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.12.0/opentelemetry-javaagent.jar) in the project root folder.
Then you can run the tests:
```bash
./gradlew -p system-tests/tests test -DincludeTags="OpenTelemetryIntegrationTest"
```