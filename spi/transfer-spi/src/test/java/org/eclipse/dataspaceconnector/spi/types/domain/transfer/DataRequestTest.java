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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DataRequestTest {

    @Test
    void verifyNoDestination() {
        var id = UUID.randomUUID().toString();
        var asset = Asset.Builder.newInstance().build();

        assertThrows(IllegalArgumentException.class, () -> DataRequest.Builder.newInstance().id(id).assetId(asset.getId()).build());
    }

    @Test
    void verifyCopy() {
        DataRequest dataRequest = newSampleDataRequest();

        var copy = dataRequest.copy();

        assertSampleDataAddress(dataRequest, copy);
    }

    @Test
    void verifyDeepCopy() {
        DataRequest dataRequest = newSampleDataRequest();

        var copy = dataRequest.copy();

        var copyProperties = copy.getProperties();
        copyProperties.put("foo", "new value");

        assertEquals("bar", dataRequest.getProperties().get("foo"));
    }

    @Test
    void verifyToBuilder() {
        DataRequest dataRequest = newSampleDataRequest();

        var copy = dataRequest.toBuilder().build();

        assertSampleDataAddress(dataRequest, copy);
    }

    private DataRequest newSampleDataRequest() {
        var properties = new HashMap<String, String>(1);
        properties.put("foo", "bar");

        var dataAddress = DataAddress.Builder
                .newInstance()
                .type("test")
                .keyName("somekey")
                .property("foo", "bar")
                .build();

        var transferType = TransferType.Builder.transferType()
                .isFinite(false)
                .contentType("someContentType")
                .build();

        return DataRequest.Builder
                .newInstance()
                .id("id")
                .processId("process-id")
                .connectorAddress("connector-address")
                .protocol("protocol")
                .connectorId("connector-id")
                .contractId("contract-id")
                .assetId("asset-id")
                .destinationType("destination-type")
                .dataDestination(dataAddress)
                .managedResources(false)    // Set sample value to false because default is true.
                .properties(properties)
                .transferType(transferType)
                .build();
    }

    private void assertSampleDataAddress(DataRequest dataRequest, DataRequest copy) {
        assertEquals(dataRequest.getId(), copy.getId());
        assertEquals(dataRequest.getProcessId(), copy.getProcessId());
        assertEquals(dataRequest.getConnectorAddress(), copy.getConnectorAddress());
        assertEquals(dataRequest.getProtocol(), copy.getProtocol());
        assertEquals(dataRequest.getConnectorId(), copy.getConnectorId());
        assertEquals(dataRequest.getAssetId(), copy.getAssetId());
        assertEquals(dataRequest.getDestinationType(), copy.getDestinationType());
        assertEquals(dataRequest.getDataDestination().getProperty("foo"), copy.getDataDestination().getProperty("foo"));
        assertEquals(dataRequest.getProperties().get("foo"), copy.getProperties().get("foo"));
        assertEquals(dataRequest.isManagedResources(), copy.isManagedResources());
        assertEquals(dataRequest.getTransferType(), copy.getTransferType());
    }
}
