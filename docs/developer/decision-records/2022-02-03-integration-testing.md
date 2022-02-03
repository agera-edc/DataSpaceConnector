# Integration testing

## Decision

Build a 

## Rationale

The need to provide an integration test harness that supports multiple runtimes emerges from multiple needs:

- Stabilizing [samples](../../samples) that run multiple connectors, which have been breaking frequently.
- Testing system behavior when multiple connectors interact, e.g. the contract negotiation process.
- Testing system behavior upon component failure.
- Providing a test facility for factoring out application components to separate runtimes (e.g. DPF).

Key drivers for the choice are:

- Fast and efficient run in CI.
- Fast inner loop and debuggability for developers.
- Use of existing frameworks, stability and portability.

We have performed technical spikes testing multiple approaches (detailed further below), including:
- Docker
- Docker compose
- Testcontainers
- JUnit

Spinning additional Class Loaders for runtimes provides very fast inner loop. Using the Gradle Classpath
is DRY and ensures the runtime under test exactly matches the standalone one.

In contrast, approaches based on Docker have a slow inner loop and require rebuild between runs.
Support for bidirectional communication with Testcontainers is clunky and complex.

The approach used is not limited to the Dataspace Connector, it can be used to run any Java module
if required in the future.

## Spikes

We have performed technical spikes:

### Docker with testcontainers

### Docker-compose with testcontainers

### Docker-compose

### Class Loader with Shadow JAR

### Class Loader with Gradle Classpath
