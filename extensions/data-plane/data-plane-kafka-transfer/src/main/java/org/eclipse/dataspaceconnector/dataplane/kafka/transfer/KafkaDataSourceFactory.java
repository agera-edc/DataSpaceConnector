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

import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.dataplane.kafka.models.KafkaDataAddressSchema.KAFKA_PROPERTIES_PREFIX;
import static org.eclipse.dataspaceconnector.dataplane.kafka.models.KafkaDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.kafka.models.KafkaDataAddressSchema.MAX_DURATION;
import static org.eclipse.dataspaceconnector.dataplane.kafka.models.KafkaDataAddressSchema.TOPIC;

class KafkaDataSourceFactory implements DataSourceFactory {
    private final TypeManager typeManager;
    private final Monitor monitor;

    KafkaDataSourceFactory(TypeManager typeManager, Monitor monitor) {
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest dataRequest) {
        return KAFKA_TYPE.equalsIgnoreCase(dataRequest.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        Map<String, String> properties = request.getSourceDataAddress().getProperties();
        try {
            checkPropertySet(properties, TOPIC);
            checkPropertySet(properties, KAFKA_PROPERTIES_PREFIX + "bootstrap.servers");
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
        return Result.success(true);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var properties = request.getSourceDataAddress().getProperties();
        var consumerProperties = properties.entrySet().stream()
                .filter(e -> e.getKey().startsWith(KAFKA_PROPERTIES_PREFIX))
                .collect(Collectors.toMap(
                        e -> e.getKey().replaceFirst(Pattern.quote(KAFKA_PROPERTIES_PREFIX), ""),
                        e -> e.getValue()
                ));
        var maxDuration = properties.get(MAX_DURATION);
        KafkaDataSource.Builder builder = KafkaDataSource.Builder.newInstance()
                .typeManager(typeManager)
                .monitor(monitor)
                .topic(properties.get(TOPIC))
                .groupId(request.getProcessId() + ":" + request.getId())
                .consumerProperties(consumerProperties);
        if (maxDuration != null) {
            builder.maxDuration(Duration.parse(maxDuration));
        }
        return builder.build();
    }

    private void checkPropertySet(Map<String, String> properties, String key) {
        if (StringUtils.isNullOrBlank(properties.get(key))) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
    }
}
