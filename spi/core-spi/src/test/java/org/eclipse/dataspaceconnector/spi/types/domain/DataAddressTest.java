/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.types.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class DataAddressTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        DataAddress dataAddress =  newSampleDataAddress();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, dataAddress);

        DataAddress deserialized = mapper.readValue(writer.toString(), DataAddress.class);

        assertNotNull(deserialized);

        assertEquals("test", deserialized.getType());
        assertEquals("bar", deserialized.getProperty("foo"));
    }

    @Test
    void verifyNoTypeThrowsException() {
        assertThatThrownBy(() -> DataAddress.Builder.newInstance().keyName("somekey").property("foo", "bar").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DataAddress builder missing Type property.");
    }

    @Test
    void verifyNullKeyThrowsException() {
        assertThatThrownBy(() -> DataAddress.Builder.newInstance().type("sometype").keyName("somekey").property(null, "bar").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Property key null.");
    }

    @Test
    void verifyCopy() {
        DataAddress dataAddress = newSampleDataAddress();

        var copy = dataAddress.copy();

        assertThat(copy).usingRecursiveComparison().isEqualTo(dataAddress);
    }

    @Test
    void verifyDeepCopy() {
        DataAddress dataAddress = newSampleDataAddress();

        var copy = dataAddress.copy();

        var copyProperties = copy.getProperties();
        copyProperties.put("foo", "new value");

        assertThat(dataAddress.getProperty("foo")).isEqualTo("bar");
    }

    @Test
    void verifyToBuilder() {
        DataAddress dataAddress = newSampleDataAddress();

        var copy = dataAddress.toBuilder().build();

        assertThat(copy).usingRecursiveComparison().isEqualTo(dataAddress);
    }

    private DataAddress newSampleDataAddress() {
        return DataAddress.Builder
                .newInstance()
                .type("test")
                .keyName("somekey")
                .property("foo", "bar")
                .build();
    }

}
