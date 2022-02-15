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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSourceToDataSinkTests {
    private static final String NULL_ENDPOINT = "https://example.com/sink";

    private ExecutorService executor;
    private Monitor monitor;
    static Faker faker = new Faker();

    FakeBlobAdapter fakeSource = new FakeBlobAdapter();
    FakeBlobAdapter fakeDestination = new FakeBlobAdapter();

    /**
     * Verifies a sink is able to pull data from the source without exceptions if both endpoints are functioning.
     */
    @Test
    void verifySuccessfulTransfer() {
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
        when(fakeSourceFactory.getBlobAdapter(
                NULL_ENDPOINT,
                NULL_ENDPOINT,
                fakeSource.name,
                "sekrit"
        )).thenReturn(fakeSource);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(NULL_ENDPOINT)
                .containerName(NULL_ENDPOINT)
                .sharedKey("sekrit")
                .name(fakeSource.name)
                .requestId("1")
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var fakeDestinationFactory = mock(BlobAdapterFactory.class);
        when(fakeDestinationFactory.getBlobAdapter(
                "https://example.com/sink",
                "https://example.com/sink",
                fakeSource.name,
                "sekrit"
        )).thenReturn(fakeDestination);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName("https://example.com/sink")
                .containerName("https://example.com/sink")
                .sharedKey("sekrit")
                .requestId("1")
                .blobAdapterFactory(fakeDestinationFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        assertThat(fakeDestination.out
                        .toString()
                ).isEqualTo(fakeSource.content);
    }

    /**
     * Verifies an exception thrown by the source endpoint is handled correctly.
     */
    @Test
    void verifyFailedTransferBecauseOfClient() throws Exception {

        // simulate source error
        var ce = mock(BlobAdapter.class);
        when(ce.openInputStream()).thenThrow(new RuntimeException());
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
        when(fakeSourceFactory.getBlobAdapter(
                NULL_ENDPOINT,
                NULL_ENDPOINT,
                fakeSource.name,
                "sekrit"
        )).thenReturn(ce);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(NULL_ENDPOINT)
                .containerName(NULL_ENDPOINT)
                .sharedKey("sekrit")
                .name(fakeSource.name)
                .requestId("1")
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        var blobAdapterFactory = mock(BlobAdapterFactory.class);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName("https://example.com/sink")
                .containerName(NULL_ENDPOINT)
                .sharedKey("sekrit")
                .requestId("1")
                .blobAdapterFactory(blobAdapterFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().failed()).isTrue();
    }


    /**
     * Verifies an exception thrown by the sink endpoint is handled correctly.
     */
    @Test
    void verifyFailedTransferBecauseOfProvider() throws Exception {

        // source completes normally
        var fakeSourceFactory = mock(BlobAdapterFactory.class);
        when(fakeSourceFactory.getBlobAdapter(
                NULL_ENDPOINT,
                NULL_ENDPOINT,
                fakeSource.name,
                "sekrit"
        )).thenReturn(fakeSource);

        var dataSource = AzureStorageDataSource.Builder.newInstance()
                .accountName(NULL_ENDPOINT)
                .containerName(NULL_ENDPOINT)
                .sharedKey("sekrit")
                .name(fakeSource.name)
                .requestId("1")
                .retryPolicy(new RetryPolicy<>())
                .blobAdapterFactory(fakeSourceFactory)
                .monitor(monitor)
                .build();

        // sink endpoint raises an exception
        var ce = mock(BlobAdapter.class);
        when(ce.getOutputStream()).thenThrow(new RuntimeException());
        var fakeDestinationFactory = mock(BlobAdapterFactory.class);
        when(fakeDestinationFactory.getBlobAdapter(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ce);

        var dataSink = AzureStorageDataSink.Builder.newInstance()
                .accountName(NULL_ENDPOINT)
                .containerName(NULL_ENDPOINT)
                .sharedKey("sekrit")
                .requestId("1")
                .blobAdapterFactory(fakeDestinationFactory)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().failed()).isTrue();
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        monitor = mock(Monitor.class);
    }

    private class FakeBlobAdapter implements BlobAdapter
    {
        private final String name= faker.lorem().characters();
        private final String content= faker.lorem().sentence();
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
            return 0;
        }
    }
}
