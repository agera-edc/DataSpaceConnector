# EDC extension for Jersey instrumentation with Micrometer

This extension uses the [Micrometer](https://micrometer.io/) metrics library to automatically collect metrics from the host system, JVM, and frameworks and libraries used in EDC (including OkHttp and ExecutorService).

## Configuration

The following properties can use used to configure the metrics that this extension will collect:

- `metrics.enabled`: enables/disables metrics collection. Default is "true".
- `metrics.jersey.enabled`: enables/disables collection of Jersey metrics