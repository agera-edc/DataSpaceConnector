# Micrometer metrics

## Decision

Use the [Micrometer](https://micrometer.io/) metrics library to automatically collect metrics from the host system, JVM, and frameworks and libraries used in EDC (including OkHttp, Jetty and Jersey). This enables users to integrate with most observability stacks in the market.

## Rationale

Capturing key system metrics, especially on I/O interfaces, is essential to system observability. Micrometer is a mature framework for collecting metrics, is well supported by vendors of metrics collector products, and can directly integrate with the popular open-source libraries used in EDC.

The [OpenTelemetry Metrics](https://opentelemetry.io/docs/reference/specification/metrics/) standard was also considered, but it is currently experimental and still lacks broad vendor and library support. It is likely to catch up in the midterm future, at which point Micrometer might be swapped.

## Spikes

