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
package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.eclipse.dataspaceconnector.common.stream.PartitionIterator;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspaceconnector.common.async.AsyncUtils.asyncAllOf;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Writes data in parallel.
 */
public abstract class ParallelSink implements DataSink {
    protected String requestId;
    protected int partitionSize = 5;
    protected ExecutorService executorService;
    protected Monitor monitor;

    @Override
    public CompletableFuture<TransferResult> transfer(DataSource source) {
        try (var partStream = source.openPartStream()) {
            var partitioned = PartitionIterator.streamOf(partStream, partitionSize);
            var futures = partitioned.map(parts -> supplyAsync(() -> transferParts(parts), executorService)).collect(toList());
            return futures.stream()
                    .collect(asyncAllOf())
                    .thenApply(results -> {
                        return results.stream()
                                .filter(AbstractResult::failed)
                                .findFirst()
                                .map(r -> TransferResult.failure(ERROR_RETRY, String.join(",", r.getFailureMessages())))
                                .orElse(TransferResult.success());
                    })
                    .exceptionally(throwable -> TransferResult.failure(ERROR_RETRY, "Unhandled exception raised when transferring data: " + throwable.getMessage()));
        } catch (Exception e) {
            monitor.severe("Error processing data transfer request: " + requestId, e);
            return CompletableFuture.completedFuture(TransferResult.failure(ERROR_RETRY, "Error processing data transfer request"));
        }
    }

    protected abstract TransferResult transferParts(List<DataSource.Part> parts);

    protected abstract static class Builder<B extends Builder<B, T>, T extends ParallelSink> {
        protected T sink;

        protected Builder(T sink) {
            this.sink = sink;
        }

        public B requestId(String requestId) {
            sink.requestId = requestId;
            return self();
        }

        public B partitionSize(int partitionSize) {
            sink.partitionSize = partitionSize;
            return self();
        }

        public B executorService(ExecutorService executorService) {
            sink.executorService = executorService;
            return self();
        }

        public B monitor(Monitor monitor) {
            sink.monitor = monitor;
            return self();
        }

        public T build() {
            Objects.requireNonNull(sink.requestId, "requestId");
            Objects.requireNonNull(sink.executorService, "executorService");
            validate();
            return sink;
        }

        protected abstract void validate();

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
}
