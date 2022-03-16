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

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AzureStorageTestFixtures {

    private static final Faker FAKER = new Faker();

    public static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .processId(FAKER.internet().uuid())
                .sourceDataAddress(createDataAddress(type).build())
                .destinationDataAddress(createDataAddress(type).build());
    }

    public static DataAddress.Builder createDataAddress(String type) {
        return DataAddress.Builder.newInstance().type(type);
    }

    public static String createAccountName() {
        return FAKER.lorem().characters(3, 24, false, false);
    }

    public static String createContainerName() {
        return FAKER.lorem().characters(3, 40, false, false);
    }

    public static String createBlobName() {
        return FAKER.lorem().characters(3, 40, false, false);
    }

    public static String createSharedKey() {
        return FAKER.lorem().characters();
    }

    private AzureStorageTestFixtures() {
    }

    static class FakeBlobAdapter implements BlobAdapter {
        final String name = FAKER.lorem().characters();
        final String content = FAKER.lorem().sentence();
        final long length = FAKER.random().nextLong(1_000_000_000_000_000L);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(content.getBytes(UTF_8));
        }

        @Override
        public String getBlobName() {
            return name;
        }

        @Override
        public long getBlobSize() {
            return length;
        }
    }

    static class TestCustomException extends RuntimeException {
        TestCustomException(String message) {
            super(message);
        }
    }
}
