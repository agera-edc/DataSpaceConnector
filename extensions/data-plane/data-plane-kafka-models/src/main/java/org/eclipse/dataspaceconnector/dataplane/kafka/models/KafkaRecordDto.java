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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = KafkaRecordDto.Builder.class)
public class KafkaRecordDto {
    private String key;
    private String value;
    private long timestamp;

    private KafkaRecordDto() {
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final KafkaRecordDto kafkaDto;

        private Builder() {
            kafkaDto = new KafkaRecordDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder key(String key) {
            kafkaDto.key = key;
            return this;
        }

        public Builder value(String value) {
            kafkaDto.value = value;
            return this;
        }

        public Builder timestamp(long timestamp) {
            kafkaDto.timestamp = timestamp;
            return this;
        }

        public KafkaRecordDto build() {
            return kafkaDto;
        }
    }
}
