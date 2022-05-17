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

package org.eclipse.dataspaceconnector.dataplane.kafka.models;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaDataAddressSchema {

    /**
     * The transfer type.
     */
    String KAFKA_TYPE = "Kafka";

    /**
     * The Kafka topic.
     */
    String TOPIC = "topic";

    /**
     * The prefix for Kafka properties. These properties are passed to the Kafka which is removed. For example, a property named {@code kafka.key.deserializer} will
     * be passed to the Kafka client as {@code key.deserializer}.
     */
    String KAFKA_PROPERTIES_PREFIX = "kafka.";

    /**
     * The maximum duration for consuming data, after which the transfer stops (optional).
     *
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String MAX_DURATION = "maxDuration";
}
