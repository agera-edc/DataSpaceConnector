# EDC extension for instrumentation with Micrometer

This extension uses the [Micrometer](https://micrometer.io/) metrics library to automatically collect metrics from the host system, JVM, and frameworks and libraries used in EDC (including OkHttp, Jetty and Jersey).

## Configuration

The following properties can use used to configure the metrics that this extension will collect:

- `metrics.enabled`: enables/disables metrics collection. Default is "true".
- `metrics.system.enabled`: enables/disables collection of system metrics (class loader, memory, garbage collection, processor and thread metrics)
- `metrics.okhttp.enabled`: enables disables collection of metrics for the OkHttp client
- `metrics.executor.enabled`: enables disables collection of metrics for the instrumented ExecutorServices (at the moment only the one used in `HealthCheckService`)