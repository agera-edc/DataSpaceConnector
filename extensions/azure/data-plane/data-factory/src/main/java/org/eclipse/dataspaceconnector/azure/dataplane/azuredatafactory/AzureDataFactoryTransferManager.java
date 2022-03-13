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
package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.resourcemanager.datafactory.models.PipelineResource;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Service for performing data transfers in Azure Data Factory.
 */
public class AzureDataFactoryTransferManager {
    private final Monitor monitor;
    private final Duration maxDuration;
    private final Clock clock;
    private final DataFactoryClient client;
    private final DataFactoryPipelineFactory pipelineFactory;

    public AzureDataFactoryTransferManager(Monitor monitor, DataFactoryClient client, DataFactoryPipelineFactory pipelineFactory, Duration maxDuration, Clock clock) {
        this.monitor = monitor;
        this.client = client;
        this.pipelineFactory = pipelineFactory;
        this.maxDuration = maxDuration;
        this.clock = clock;
    }

    /**
     * Transfers data from source to destination.
     */
    public CompletableFuture<TransferResult> transfer(DataFlowRequest request) {

        PipelineResource pipeline = pipelineFactory.createPipeline(request);

        var runId = client.runPipeline(pipeline).runId();

        monitor.info("Created ADF pipeline for " + request.getProcessId() + ". Run id is " + runId);

        return awaitRunCompletion(runId);
    }

    @NotNull
    private CompletableFuture<TransferResult> awaitRunCompletion(String runId) {
        monitor.info("Awaiting ADF pipeline completion for run " + runId);

        var timeout = clock.instant().plus(maxDuration);
        while (clock.instant().isBefore(timeout)) {
            var pipelineRun = client.getPipelineRun(runId);
            var runStatusValue = pipelineRun.status();
            var message = pipelineRun.message();
            monitor.info("ADF run status is " + runStatusValue + " with message [" + message + "] for run " + runId);
            DataFactoryPipelineRunStates runStatus;

            try {
                runStatus = DataFactoryPipelineRunStates.valueOf(runStatusValue);
            } catch (IllegalArgumentException e) {
                return completedFuture(TransferResult.failure(ERROR_RETRY,
                        format("ADF run in unexpected state %s with message: %s", runStatusValue, message)));
            }
            if (runStatus.succeeded) {
                return completedFuture(TransferResult.success());
            }
            if (runStatus.failed) {
                return completedFuture(TransferResult.failure(ERROR_RETRY,
                        format("ADF run in state %s with message: %s", runStatusValue, message)));
            }
        }
        client.cancelPipelineRun(runId);
        return completedFuture(TransferResult.failure(ERROR_RETRY, "ADF run timed out"));
    }

    @SuppressWarnings("unused")
    private enum DataFactoryPipelineRunStates {
        Queued(false, false),
        InProgress(false, false),
        Succeeded(true, false),
        Failed(false, true),
        Canceling(false, true),
        Cancelled(false, true);

        final boolean succeeded;
        final boolean failed;

        DataFactoryPipelineRunStates(boolean succeeded, boolean failed) {
            this.succeeded = succeeded;
            this.failed = failed;
        }
    }
}
