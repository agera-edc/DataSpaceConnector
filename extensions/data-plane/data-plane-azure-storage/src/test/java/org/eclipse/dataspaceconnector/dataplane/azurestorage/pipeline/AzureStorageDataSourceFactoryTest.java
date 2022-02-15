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
package org.eclipse.dataspaceconnector.dataplane.azurestorage.pipeline;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AzureStorageDataSourceFactoryTest {
    private AzureStorageDataSourceFactory factory;

    @Test
    void verifyCanHandle() {
        DataFlowRequest azureStorageRequest = createRequest(TYPE).build();
        DataFlowRequest nonAzureStorageRequest = createRequest("Unknown").build();

        assertThat(factory.canHandle(azureStorageRequest)).isTrue();
        assertThat(factory.canHandle(nonAzureStorageRequest)).isFalse();
    }

    @Test
    void verifyValidation() {
        var dataAddress = DataAddress.Builder.newInstance().property(ACCOUNT_NAME, "http://example.com").property(BLOB_NAME, "foo").type(TYPE).build();
        var validRequest = createRequest(TYPE).sourceDataAddress(dataAddress).build();
        assertThat(factory.validate(validRequest).succeeded()).isTrue();

        var missingEndpointRequest = createRequest("Unknown").build();
        assertThat(factory.validate(missingEndpointRequest).failed()).isTrue();

        var missingNameAddress = DataAddress.Builder.newInstance().property(ACCOUNT_NAME, "http://example.com").type(TYPE).build();
        var missingNameRequest = createRequest(TYPE).sourceDataAddress(missingNameAddress).build();
        assertThat(factory.validate(missingNameRequest).failed()).isTrue();
    }

    @Test
    void verifyCreateSource() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, "http://example.com")
                .property(CONTAINER_NAME, "http://example.com")
                .property(BLOB_NAME, "foo.json")
                .property(SHARED_KEY, "foo.json")
                .build();


        var missingEndpointAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, "http://example.com")
                .build();

        var missingNameAddress = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(BLOB_NAME, "foo.json")
                .build();

        var validRequest = createRequest(TYPE).sourceDataAddress(dataAddress).build();
        var missingEndpointRequest = createRequest(TYPE).sourceDataAddress(missingEndpointAddress).build();
        var missingNameRequest = createRequest(TYPE).sourceDataAddress(missingNameAddress).build();


        assertThat(factory.createSource(validRequest)).isNotNull();
        assertThrows(EdcException.class, () -> factory.createSource(missingEndpointRequest));
        assertThrows(EdcException.class, () -> factory.createSource(missingNameRequest));
    }

    @BeforeEach
    void setUp() {
        factory = new AzureStorageDataSourceFactory(mock(BlobAdapterFactory.class), new RetryPolicy<>(), mock(Monitor.class));
    }


}
