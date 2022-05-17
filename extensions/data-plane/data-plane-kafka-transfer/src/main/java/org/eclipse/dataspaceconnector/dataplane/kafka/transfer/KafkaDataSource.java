/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.kafka.transfer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.dataspaceconnector.dataplane.kafka.models.KafkaRecordDto;
import org.eclipse.dataspaceconnector.dataplane.kafka.models.KafkaRecordsDto;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class KafkaDataSource implements DataSource {

    private TypeManager typeManager;
    private Monitor monitor;
    private String topic;
    private String groupId;
    private Duration maxDuration;
    private Map<String, String> consumerProperties = Map.of();

    @NotNull
    private Consumer<?, ?> createKafkaConsumer() {
        var props = new Properties();
        props.putAll(consumerProperties);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        var consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    @Override
    public Stream<Part> openPartStream() {
        return openRecordsStream().map(this::toPart);
    }

    @NotNull
    private Stream<ConsumerRecords<?, ?>> openRecordsStream() {
        var consumer = createKafkaConsumer();

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new ConsumerRecordsIterator(consumer, maxDuration), 0),
                /* not parallel */ false);
    }

    private Part toPart(ConsumerRecords<?, ?> consumerRecords) {
        return new Part() {
            @Override
            public String name() {
                return topic;
            }

            @Override
            public InputStream openStream() {
                var records = consumerRecords.partitions().stream()
                        .flatMap(p -> consumerRecords.records(p).stream())
                        .map(KafkaDataSource::mapConsumerRecordToDto)
                        .collect(Collectors.toList());
                var result = KafkaRecordsDto.Builder.newInstance()
                        .topic(topic)
                        .records(records)
                        .build();
                return new ByteArrayInputStream(typeManager.writeValueAsBytes(result));
            }
        };
    }

    private static KafkaRecordDto mapConsumerRecordToDto(ConsumerRecord<?, ?> record) {
        return KafkaRecordDto.Builder.newInstance()
                .key(record.key())
                .value(record.value())
                .timestamp(record.timestamp())
                .build();
    }

    private KafkaDataSource() {
    }

    public static class Builder {
        private final KafkaDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder typeManager(TypeManager typeManager) {
            dataSource.typeManager = typeManager;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }


        public Builder topic(String topic) {
            dataSource.topic = topic;
            return this;
        }

        public Builder groupId(String groupId) {
            dataSource.groupId = groupId;
            return this;
        }

        public Builder maxDuration(Duration maxDuration) {
            dataSource.maxDuration = maxDuration;
            return this;
        }

        public Builder consumerProperties(Map<String, String> consumerProperties) {
            dataSource.consumerProperties = Map.copyOf(consumerProperties);
            return this;
        }

        public KafkaDataSource build() {
            Objects.requireNonNull(dataSource.typeManager, "typeManager");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.topic, "topic");
            Objects.requireNonNull(dataSource.groupId, "groupId");
            return dataSource;
        }

        private Builder() {
            dataSource = new KafkaDataSource();
        }
    }

    private class ConsumerRecordsIterator implements Iterator<ConsumerRecords<?, ?>> {
        private final Consumer<?, ?> consumer;
        private final Duration maxDuration;
        private Instant consumeUntil;
        private final Clock clock = Clock.systemUTC();

        ConsumerRecordsIterator(Consumer<?, ?> consumer, Duration maxDuration) {
            this.consumer = consumer;
            this.maxDuration = maxDuration;
        }

        @Override
        public boolean hasNext() {
            if (consumeUntil == null) {
                return true;
            }
            boolean maxDurationReached = getTimeLeft().isNegative();
            if (maxDurationReached) {
                monitor.debug("Kafka consumer maximum duration reached");
            }
            return !maxDurationReached;
        }

        private Duration getTimeLeft() {
            return Duration.between(clock.instant(), consumeUntil);
        }

        @Override
        public ConsumerRecords<?, ?> next() {
            if (consumeUntil == null) {
                consumeUntil = maxDuration == null ? Instant.MAX : clock.instant().plus(maxDuration);
            }
            var records = consumer.poll(Duration.ZERO);
            while (records.isEmpty()) {
                var timeLeft = getTimeLeft();
                if (timeLeft.isNegative()) {
                    break;
                }
                records = consumer.poll(min(timeLeft, Duration.ofSeconds(1)));
            }
            return records;
        }
    }

    private static <T extends Comparable<T>> T min(T a, T b) {
        return a.compareTo(b) < 0 ? a : b;
    }
}
