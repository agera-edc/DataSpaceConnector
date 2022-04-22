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

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter;

import com.azure.core.credential.AzureSasCredential;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for {@link BlobAdapter}.
 */
public class BlobAdapterFactory {
    private final String blobstoreEndpointTemplate;

    public BlobAdapterFactory(String blobstoreEndpointTemplate) {
        this.blobstoreEndpointTemplate = blobstoreEndpointTemplate;
    }

    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, String sharedKey) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder().credential(new StorageSharedKeyCredential(accountName, sharedKey));
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, AzureSasCredential credential) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder().credential(credential);
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    @NotNull
    private BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, BlobServiceClientBuilder builder) {
        var blobServiceClient = builder
                .endpoint(createEndpoint(accountName))
                .buildClient();

        var blockBlobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName)
                .getBlockBlobClient();

        return new DefaultBlobAdapter(blockBlobClient);
    }

    private String createEndpoint(String accountName) {
        return String.format(blobstoreEndpointTemplate, accountName);
    }

}

