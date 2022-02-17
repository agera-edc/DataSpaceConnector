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
package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.ParallelSink;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Writes data into an Azure storage container.
 */
public class AzureStorageDataSink extends ParallelSink {
    private String accountName;
    private String containerName;
    private String sharedKey;
    private BlobAdapterFactory blobAdapterFactory;

    /**
     * Writes data into an Azure storage container.
     */
    protected TransferResult transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            String blobName = part.name();
            try (var input = part.openStream()) {
                try (var output = blobAdapterFactory.getBlobAdapter(accountName, containerName, blobName, sharedKey)
                        .getOutputStream()) {
                    try {
                        input.transferTo(output);
                    } catch (Exception e) {
                        return errorResult(format("Error transferring blob stream for %s on account %s", blobName, accountName),
                                "Error copying Azure Storage blob", e);
                    }
                } catch (Exception e) {
                    return errorResult(format("Error creating blob for %s on account %s", blobName, accountName),
                            "Error writing Azure Storage blob", e);
                }
            } catch (Exception e) {
                return errorResult(format("Error reading blob %s", blobName),
                        "Error reading Azure Storage blob", e);
            }
        }
        return TransferResult.success();
    }

    @NotNull
    private TransferResult errorResult(String logMessage, String errorMessage, Exception e) {
        monitor.severe(logMessage, e);
        return TransferResult.failure(ERROR_RETRY, errorMessage);
    }

    private AzureStorageDataSink() {
    }

    public static class Builder extends ParallelSink.Builder<Builder, AzureStorageDataSink> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accountName(String accountName) {
            sink.accountName = accountName;
            return this;
        }

        public Builder containerName(String containerName) {
            sink.containerName = containerName;
            return this;
        }

        public Builder sharedKey(String sharedKey) {
            sink.sharedKey = sharedKey;
            return this;
        }

        public Builder blobAdapterFactory(BlobAdapterFactory blobAdapterFactory) {
            sink.blobAdapterFactory = blobAdapterFactory;
            return this;
        }

        protected void validate() {
            Objects.requireNonNull(sink.accountName, "accountName");
            Objects.requireNonNull(sink.containerName, "containerName");
            Objects.requireNonNull(sink.sharedKey, "sharedKey");
            Objects.requireNonNull(sink.blobAdapterFactory, "blobAdapterFactory");
        }

        private Builder() {
            super(new AzureStorageDataSink());
        }
    }
}
