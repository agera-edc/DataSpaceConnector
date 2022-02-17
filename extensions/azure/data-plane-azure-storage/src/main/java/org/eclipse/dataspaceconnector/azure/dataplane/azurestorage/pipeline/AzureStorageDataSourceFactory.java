/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  ~s://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Instantiates {@link AzureStorageDataSource}s for requests whose source data type is {@link AzureBlobStoreSchema#TYPE}.
 */
public class AzureStorageDataSourceFactory implements DataSourceFactory {
    private final BlobAdapterFactory blobAdapterFactory;
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;

    public AzureStorageDataSourceFactory(BlobAdapterFactory blobAdapterFactory, RetryPolicy<Object> retryPolicy, Monitor monitor) {
        this.blobAdapterFactory = blobAdapterFactory;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var dataAddress = request.getSourceDataAddress();
        var properties = dataAddress.getProperties();
        if (!properties.containsKey(AzureBlobStoreSchema.ACCOUNT_NAME)) {
            return Result.failure(String.format("Azure Blob data source %s not provided for request %s", AzureBlobStoreSchema.ACCOUNT_NAME, request.getId()));
        }
        if (!properties.containsKey(AzureBlobStoreSchema.ACCOUNT_NAME)) {
            return Result.failure(String.format("Azure Blob data source %s not provided for request %s", AzureBlobStoreSchema.ACCOUNT_NAME, request.getId()));
        }
        if (!properties.containsKey(AzureBlobStoreSchema.CONTAINER_NAME)) {
            return Result.failure(String.format("Azure Blob data source %s not provided for request %s", AzureBlobStoreSchema.CONTAINER_NAME, request.getId()));
        }
        if (!properties.containsKey(AzureBlobStoreSchema.BLOB_NAME)) {
            return Result.failure(String.format("Azure Blob data source %s not provided for request %s", AzureBlobStoreSchema.BLOB_NAME, request.getId()));
        }
        if (!properties.containsKey(AzureBlobStoreSchema.SHARED_KEY)) {
            return Result.failure(String.format("Azure Blob data source %s not provided for request %s", AzureBlobStoreSchema.SHARED_KEY, request.getId()));
        }
        return VALID;
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        Result<Boolean> validate = validate(request);
        if (validate.failed()) {
            throw new EdcException(validate.getFailure().getMessages().toString());
        }
        var dataAddress = request.getSourceDataAddress();

        return AzureStorageDataSource.Builder.newInstance()
                .accountName(dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME))
                .containerName(dataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                .sharedKey(dataAddress.getProperty(AzureBlobStoreSchema.SHARED_KEY))
                .blobAdapterFactory(blobAdapterFactory)
                .blobName(dataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                .requestId(request.getId())
                .retryPolicy(retryPolicy)
                .monitor(monitor)
                .build();
    }
}
