# EDC extension for Jetty instrumentation with Micrometer

This extension uses the [Micrometer](https://micrometer.io/) metrics library to automatically collect metrics from Jetty.

## Configuration

The following properties can use used to configure the metrics that this extension will collect:

- `metrics.enabled`: enables/disables metrics collection. Default is "true".
- `metrics.jetty.enabled`: enables/disables collection of Jetty metrics