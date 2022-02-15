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

import static java.lang.String.format;
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
            return failure(format("Azure Blob data sink %s not provided for request %s", ACCOUNT_NAME, request.getId()));
        }
        if (dataAddress == null || !dataAddress.getProperties().containsKey(CONTAINER_NAME)) {
            return failure(format("Azure Blob data sink %s not provided for request %s", CONTAINER_NAME, request.getId()));
        }
        if (dataAddress == null || !dataAddress.getProperties().containsKey(SHARED_KEY)) {
            return failure(format("Azure Blob data sink %s not provided for request %s", SHARED_KEY, request.getId()));
        }
        return VALID;
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        var dataAddress = request.getDestinationDataAddress();
        var requestId = request.getId();
        Result<Boolean> validate = validate(request);
        if (validate.failed()) {
            throw new EdcException(validate.getFailure().getMessages().toString());
        }

        return AzureStorageDataSink.Builder.newInstance()
                .accountName(dataAddress.getProperty(ACCOUNT_NAME))
                .containerName(dataAddress.getProperty(CONTAINER_NAME))
                .sharedKey(dataAddress.getProperty(SHARED_KEY))
                .requestId(requestId)
                .partitionSize(partitionSize)
                .blobAdapterFactory(blobAdapterFactory)
                .executorService(executorService)
                .monitor(monitor)
                .build();
    }
}
