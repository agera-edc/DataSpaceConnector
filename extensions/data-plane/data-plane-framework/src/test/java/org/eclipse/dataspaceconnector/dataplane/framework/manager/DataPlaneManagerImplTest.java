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
package org.eclipse.dataspaceconnector.dataplane.framework.manager;

import org.eclipse.dataspaceconnector.dataplane.framework.store.InMemoryDataPlaneStore;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

class DataPlaneManagerImplTest {
    private TransferService transferService = mock(TransferService.class);
    private TransferService transferService2 = mock(TransferService.class);

    DataPlaneStore store = new InMemoryDataPlaneStore(10);
    DataFlowRequest request = createRequest();
    CountDownLatch latch = new CountDownLatch(1);
    ArgumentCaptor<Stream> streamCaptor = ArgumentCaptor.forClass(Stream.class);
    TransferServiceSelectionStrategy transferServiceSelectionStrategy = mock(TransferServiceSelectionStrategy.class);

    {
        when(transferServiceSelectionStrategy.chooseTransferService(any(), any()))
                .thenReturn(transferService);
    }

    /**
     * Verifies a request is enqueued, dequeued, and dispatched to the pipeline service.
     */
    @Test
    void verifyWorkDispatch() throws InterruptedException {
        var dataPlaneManager = extracted();

        when(transferService.canHandle(isA(DataFlowRequest.class)))
                .thenReturn(true);

        when(transferService.transfer(isA(DataFlowRequest.class))).thenAnswer(i -> {
            latch.countDown();
            return completedFuture(Result.success("ok"));
        });


        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);

        assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();

        dataPlaneManager.stop();

        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
        assertThat(streamCaptor.getValue()).containsExactly(transferService);
        verify(transferService2).canHandle(isA(DataFlowRequest.class));

        verify(transferService).transfer(isA(DataFlowRequest.class));
    }

    /**
     * Verifies that the dispatch thread survives an error thrown by a worker.
     */
    @Test
    void verifyWorkDispatchError() throws InterruptedException {
        var dataPlaneManager = extracted();

        when(transferService.canHandle(request))
                .thenReturn(true);

        when(transferService.transfer(request))
                .thenAnswer(i -> {
                    throw new RuntimeException("Test exception");
                }).thenAnswer((i -> {
                    latch.countDown();
                    return completedFuture(Result.success("ok"));
                }));


        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);
        dataPlaneManager.initiateTransfer(request);

        assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();

        dataPlaneManager.stop();

        verify(transferService, times(2)).transfer(request);
    }

    @Test
    void verifyWorkDispatch_onUnavailableTransferService_completesTransfer() throws InterruptedException {
        store = mock(DataPlaneStore.class);

        var dataPlaneManager = extracted();

        when(transferService.canHandle(isA(DataFlowRequest.class)))
                .thenReturn(false);

        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).completed(request.getProcessId());

        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);

        assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();

        dataPlaneManager.stop();
    }


    private DataPlaneManagerImpl extracted() {
        var monitor = mock(Monitor.class);
        var dataPlaneManager = DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(100)
                .workers(1)
                .waitTimeout(10)
                .transferServiceSelectionStrategy(transferServiceSelectionStrategy)
                .store(store)
                .monitor(monitor).build();
        dataPlaneManager.registerTransferService(transferService);
        dataPlaneManager.registerTransferService(transferService2);
        return dataPlaneManager;
    }

    DataFlowRequest createRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .build();
    }

}
