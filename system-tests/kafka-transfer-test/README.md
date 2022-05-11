# Kafka transfer

## Prerequisites

If running on a Mac M1, copy `.env.example`to `.env` and uncomment the `PREFIX` line.

## Run the tests

We will use a single docker-compose to run Kafka.
Let's have a look to the [docker-compose.yaml](docker-compose.yaml). We created the Kafka service.

To run Kafka execute the following commands in the project root folder:

```bash
./gradlew system-tests:kafka-transfer-test:consumer:build system-tests:kafka-transfer-test:provider:build
docker-compose -f system-tests/kafka-transfer-test/docker-compose.yaml up --abort-on-container-exit
```

The docker-compose file also runs [`kcat`](https://github.com/edenhill/kcat) as a debugging tool to output all incoming messages.
The topic is automatically created by system tests. Until those are run, `kcat` will output errors
which you can ignore:
```
kcat       | % ERROR: Topic test_events error: Broker: Unknown topic or partition
```

Once Kafka is up, run system tests.
