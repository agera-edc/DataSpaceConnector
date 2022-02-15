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

import org.eclipse.dataspaceconnector.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.SHARED_KEY;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AzureStorageDataSinkFactoryTest {
    private AzureStorageDataSinkFactory factory;
    private BlobAdapterFactory blobAdapterFactory;

    @Test
    void verifyCanHandle() {
        var azureStorageRequest = createRequest(AzureBlobStoreSchema.TYPE).build();
        var nonAzureStorageRequest = createRequest("Unknown").build();

        assertThat(factory.canHandle(azureStorageRequest)).isTrue();
        assertThat(factory.canHandle(nonAzureStorageRequest)).isFalse();
    }

    @Test
    void verifyValidation() {
        var dataAddress = DataAddress.Builder.newInstance()
                .property(ACCOUNT_NAME, "http://example.com")
                .property(CONTAINER_NAME, "http://example.com")
                .type(TYPE)
                .build();
        var validRequest = createRequest(TYPE).destinationDataAddress(dataAddress).build();
        assertThat(factory.validate(validRequest).succeeded()).isTrue();

        var missingEndpointRequest = createRequest("Unknown").build();
        assertThat(factory.validate(missingEndpointRequest).failed()).isTrue();
    }

    @Test
    void verifyCreateSource() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type(AzureBlobStoreSchema.TYPE)
                .property(ACCOUNT_NAME, "http://example.com")
                .property(CONTAINER_NAME, "http://example.com")
                .property(SHARED_KEY, "sekrit")
                .build();
        var validRequest = createRequest(AzureBlobStoreSchema.TYPE).destinationDataAddress(dataAddress).build();
        var missingEndpointRequest = createRequest("Unknown").build();

        assertThat(factory.createSink(validRequest)).isNotNull();
        assertThrows(EdcException.class, () -> factory.createSink(missingEndpointRequest));
    }

    @BeforeEach
    void setUp() {
        blobAdapterFactory = mock(BlobAdapterFactory.class);
        factory = new AzureStorageDataSinkFactory(blobAdapterFactory, Executors.newFixedThreadPool(1), 5, mock(Monitor.class));
    }


}
