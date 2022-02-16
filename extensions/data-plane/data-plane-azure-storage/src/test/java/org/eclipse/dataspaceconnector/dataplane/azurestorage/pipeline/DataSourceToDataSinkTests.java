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

import com.github.javafaker.Faker;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSourceToDataSinkTests {
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private Monitor monitor = mock(Monitor.class);

    static Faker faker = new Faker();

    FakeBlobAdapter fakeSource = new FakeBlobAdapter();
    FakeBlobAdapter fakeSink = new FakeBlobAdapter();

    String sourceAccountName = createAccountName();
    String sourceContainerName = createContainerName();
    String sourceSharedKey = createSharedKey();
    String sinkAccountName = createAccountName();
    String sinkContainerName = createContainerName();
    String sinkSharedKey = createSharedKey();
    String requestId = UUID.randomUUID().toString();
    String errorMessage = faker.lorem().sentence();

    /**
     * Verifies a sink is able to pull data from the source without exceptions if both endpoints are functioning.
     */
    @Test
    void transfer_success() {
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
        when(fakeSourceFactory.getBlobAdapter(
                sourceAccountName,
                sourceContainerName,
                fakeSource.name,
                sourceSharedKey
        )).thenReturn(fakeSource);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(sourceAccountName)
                .containerName(sourceContainerName)
                .sharedKey(sourceSharedKey)
                .name(fakeSource.name)
                .requestId(requestId)
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var fakeSinkFactory = mock(BlobAdapterFactory.class);
        when(fakeSinkFactory.getBlobAdapter(
                sinkAccountName,
                sinkContainerName,
                fakeSource.name,
                sinkSharedKey
        )).thenReturn(fakeSink);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedKey(sinkSharedKey)
                .requestId(requestId)
                .blobAdapterFactory(fakeSinkFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        assertThat(fakeSink.out.toString()).isEqualTo(fakeSource.content);
    }

    /**
     * Verifies an exception thrown by the source endpoint is handled correctly.
     */
    @Test
    void transfer_WhenSourceFails_fails() throws Exception {

        // simulate source error
        var ce = mock(BlobAdapter.class);
        when(ce.getBlobName()).thenReturn(fakeSource.name);
        when(ce.openInputStream()).thenThrow(new RuntimeException(errorMessage));
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
        when(fakeSourceFactory.getBlobAdapter(
                sourceAccountName,
                sourceContainerName,
                fakeSource.name,
                sourceSharedKey
        )).thenReturn(ce);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(sourceAccountName)
                .containerName(sourceContainerName)
                .sharedKey(sourceSharedKey)
                .name(fakeSource.name)
                .requestId(requestId)
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var fakeSinkFactory = mock(BlobAdapterFactory.class);
        when(fakeSinkFactory.getBlobAdapter(
                sinkAccountName,
                sinkContainerName,
                fakeSource.name,
                sinkSharedKey
        )).thenReturn(fakeSink);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedKey(sinkSharedKey)
                .requestId(requestId)
                .blobAdapterFactory(fakeSinkFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        TransferResult transferResult = dataSink.transfer(dataSource).get();
        assertThat(transferResult.failed()).isTrue();
        assertThat(transferResult.getFailureMessages()).containsExactly(
                format("Unhandled exception raised when transferring data: java.lang.RuntimeException: %s", errorMessage));
    }


    /**
     * Verifies an exception thrown by the sink endpoint is handled correctly.
     */
    @Test
    void transfer_whenSinkFails_fails() throws Exception {

        // source completes normally
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
        when(fakeSourceFactory.getBlobAdapter(
                sourceAccountName,
                sourceContainerName,
                fakeSource.name,
                sourceSharedKey
        )).thenReturn(fakeSource);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(sourceAccountName)
                .containerName(sourceContainerName)
                .sharedKey(sourceSharedKey)
                .name(fakeSource.name)
                .requestId(requestId)
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        // sink endpoint raises an exception
        var ce = mock(BlobAdapter.class);
        when(ce.getOutputStream()).thenThrow(new RuntimeException());
        var fakeSinkFactory = mock(BlobAdapterFactory.class);
        when(fakeSinkFactory.getBlobAdapter(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ce);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(sinkAccountName)
                .containerName(sinkContainerName)
                .sharedKey(sinkSharedKey)
                .requestId(requestId)
                .blobAdapterFactory(fakeSinkFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().failed()).isTrue();
    }

    private class FakeBlobAdapter implements BlobAdapter {
        private final String name = faker.lorem().characters();
        private final String content = faker.lorem().sentence();
        private final long length = faker.random().nextLong(1_000_000_000_000_000L);
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

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
}
