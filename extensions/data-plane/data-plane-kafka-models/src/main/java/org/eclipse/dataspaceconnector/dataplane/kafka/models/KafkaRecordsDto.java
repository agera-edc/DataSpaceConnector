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

import java.util.List;

@JsonDeserialize(builder = KafkaRecordsDto.Builder.class)
public class KafkaRecordsDto {
    private String topic;
    private List<KafkaRecordDto> records;

    private KafkaRecordsDto() {
    }

    public String getTopic() {
        return topic;
    }

    public List<KafkaRecordDto> getRecords() {
        return records;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final KafkaRecordsDto kafkaRecordsDto;

        private Builder() {
            kafkaRecordsDto = new KafkaRecordsDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder topic(String topic) {
            kafkaRecordsDto.topic = topic;
            return this;
        }

        public Builder records(List<KafkaRecordDto> records) {
            kafkaRecordsDto.records = List.copyOf(records);
            return this;
        }

        public KafkaRecordsDto build() {
            return kafkaRecordsDto;
        }
    }
}
