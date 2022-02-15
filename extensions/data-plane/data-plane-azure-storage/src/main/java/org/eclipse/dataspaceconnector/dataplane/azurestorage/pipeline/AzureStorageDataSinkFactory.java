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
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.schema.AzureBlobStoreSchema.SHARED_KEY;
import static org.eclipse.dataspaceconnector.spi.result.Result.failure;

/**
 * Instantiates {@link AzureStorageDataSink}s for requests whose source data type is {@link AzureBlobStoreSchema#TYPE}.
 */
public class AzureStorageDataSinkFactory implements DataSinkFactory {
    private final BlobAdapterFactory blobAdapterFactory;
    private final ExecutorService executorService;
    private final int partitionSize;
    private final Monitor monitor;

    public AzureStorageDataSinkFactory(BlobAdapterFactory blobAdapterFactory, ExecutorService executorService, int partitionSize, Monitor monitor) {
        this.blobAdapterFactory = blobAdapterFactory;
        this.executorService = executorService;
        this.partitionSize = partitionSize;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var dataAddress = request.getDestinationDataAddress();
        if (dataAddress == null || !dataAddress.getProperties().containsKey(ACCOUNT_NAME)) {
            return failure("Azure Blob data sink account not provided for request: " + request.getId());
        }
        return VALID;
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        var dataAddress = request.getDestinationDataAddress();
        var requestId = request.getId();

        String accountName = dataAddress.getProperty(ACCOUNT_NAME);
        if (accountName == null) {
            throw new EdcException("Azure Blob data destination account not provided for request: " + requestId);
        }

        String containerName = dataAddress.getProperty(CONTAINER_NAME);
        if (containerName == null) {
            throw new EdcException("Azure Blob data destination container not provided for request: " + requestId);
        }

        String sharedKey = dataAddress.getProperty(SHARED_KEY);
        if (sharedKey == null) {
            throw new EdcException("Azure Blob data destination sharedKey not provided for request: " + requestId);
        }

        return AzureStorageDataSink.Builder.newInstance()
                .accountName(accountName)
                .containerName(containerName)
                .sharedKey(sharedKey)
                .requestId(requestId)
                .partitionSize(partitionSize)
                .blobAdapterFactory(blobAdapterFactory)
                .executorService(executorService)
                .monitor(monitor)
                .build();
    }
}
