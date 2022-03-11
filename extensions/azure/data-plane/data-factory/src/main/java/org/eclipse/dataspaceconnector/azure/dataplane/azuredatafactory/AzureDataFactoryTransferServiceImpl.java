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

import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.resourcemanager.datafactory.models.AzureBlobStorageLocation;
import com.azure.resourcemanager.datafactory.models.AzureKeyVaultSecretReference;
import com.azure.resourcemanager.datafactory.models.AzureStorageLinkedService;
import com.azure.resourcemanager.datafactory.models.BinaryDataset;
import com.azure.resourcemanager.datafactory.models.BlobSink;
import com.azure.resourcemanager.datafactory.models.BlobSource;
import com.azure.resourcemanager.datafactory.models.CopyActivity;
import com.azure.resourcemanager.datafactory.models.DatasetReference;
import com.azure.resourcemanager.datafactory.models.DatasetResource;
import com.azure.resourcemanager.datafactory.models.LinkedServiceReference;
import com.azure.resourcemanager.datafactory.models.LinkedServiceResource;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.security.keyvault.secrets.SecretClient;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateSharedKey;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * {@link TransferService} implementation that performs transfers in Azure Data Factory.
 */
public class AzureDataFactoryTransferServiceImpl implements TransferService {
    private final DataFactoryManager dataFactoryManager;
    private final GenericResource factory;
    private final Monitor monitor;
    private final String keyVaultLinkedService;
    private final SecretClient secretClient;
    private final Duration maxDuration;
    private final Clock clock;

    public AzureDataFactoryTransferServiceImpl(Monitor monitor, DataFactoryManager dataFactoryManager, GenericResource factory, SecretClient secretClient, String keyVaultLinkedService, Duration maxDuration, Clock clock) {
        this.monitor = monitor;
        this.dataFactoryManager = dataFactoryManager;
        this.factory = factory;
        this.secretClient = secretClient;
        this.keyVaultLinkedService = keyVaultLinkedService;
        this.maxDuration = maxDuration;
        this.clock = clock;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getSourceDataAddress().getType()) &&
                AzureBlobStoreSchema.TYPE.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var dataAddress = request.getDestinationDataAddress();
        var properties = new HashMap<>(dataAddress.getProperties());
        try {
            validateAccountName(properties.remove(AzureBlobStoreSchema.ACCOUNT_NAME));
            validateContainerName(properties.remove(AzureBlobStoreSchema.CONTAINER_NAME));
            validateSharedKey(properties.remove(AzureBlobStoreSchema.SHARED_KEY));
            properties.keySet().stream().filter(k -> !DataAddress.TYPE.equals(k)).findFirst().ifPresent(k -> {
                throw new IllegalArgumentException(format("Unexpected property %s", k));
            });
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
        return Result.success(true);
    }

    @Override
    public CompletableFuture<TransferResult> transfer(DataFlowRequest request) {

        var baseName = "EDC-DPF-" + UUID.randomUUID();

        monitor.info("Creating ADF pipeline for " + baseName);

        var sourceDataset = createDataset(baseName + "src", request.getSourceDataAddress());
        var destinationDataset = createDataset(baseName + "dst", request.getDestinationDataAddress());

        var pipeline = createCopyPipeline(baseName, sourceDataset, destinationDataset);

        var runId = runPipeline(pipeline);

        return awaitRunCompletion(baseName, runId);
    }

    @NotNull
    private CompletableFuture<TransferResult> awaitRunCompletion(String baseName, String runId) {
        monitor.info("Awaiting ADF pipeline completion for " + baseName);

        var timeout = clock.instant().plus(maxDuration);
        while (clock.instant().isBefore(timeout)) {
            var pipelineRun =
                    dataFactoryManager
                            .pipelineRuns()
                            .get(factory.resourceGroupName(), factory.name(), runId);
            var runStatusValue = pipelineRun.status();
            var message = pipelineRun.message();
            monitor.info("ADF pipeline status is " + runStatusValue + " with message [" + message + "] for " + baseName);
            var runStatus = DataFactoryPipelineRunStates.valueOf(runStatusValue);
            if (runStatus.succeeded) {
                return completedFuture(TransferResult.success());
            }
            if (runStatus.failed) {
                return completedFuture(TransferResult.failure(ERROR_RETRY, message));
            }
        }
        dataFactoryManager
                .pipelineRuns()
                .cancel(factory.resourceGroupName(), factory.name(), runId);
        return completedFuture(TransferResult.failure(ERROR_RETRY, "Run timed out"));
    }

    private String runPipeline(PipelineResource pipeline) {
        return dataFactoryManager.pipelines()
                .createRun(
                        factory.resourceGroupName(),
                        factory.name(),
                        pipeline.name())
                .runId();
    }

    private PipelineResource createCopyPipeline(String baseName, DatasetResource sourceDataset, DatasetResource destinationDataset) {
        return dataFactoryManager.pipelines()
                .define(baseName)
                .withExistingFactory(factory.resourceGroupName(), factory.name())
                .withActivities(List.of(new CopyActivity()
                        .withName("CopyActivity")
                        .withInputs(List.of(new DatasetReference().withReferenceName(sourceDataset.name())))
                        .withOutputs(List.of(new DatasetReference().withReferenceName(destinationDataset.name())))
                        .withSource(new BlobSource())
                        .withSink(new BlobSink())
                        .withValidateDataConsistency(true)
                        .withDataIntegrationUnits(32)))
                .create();
    }

    private DatasetResource createDataset(String name, DataAddress sourceDataAddress) {
        var linkedService = createLinkedService(name, sourceDataAddress);
        return createDatasetResource(name, linkedService, sourceDataAddress);
    }

    private DatasetResource createDatasetResource(String name, LinkedServiceResource linkedService, DataAddress dataAddress) {
        return dataFactoryManager
                .datasets()
                .define(name)
                .withExistingFactory(factory.resourceGroupName(), factory.name())
                .withProperties(
                        new BinaryDataset()
                                .withLinkedServiceName(new LinkedServiceReference().withReferenceName(linkedService.name()))
                                .withLocation(new AzureBlobStorageLocation()
                                        .withFileName(dataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                                        .withContainer(dataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                                )
                )
                .create();
    }

    private LinkedServiceResource createLinkedService(String name, DataAddress dataAddress) {
        var accountName = dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var accountKey = dataAddress.getProperty(AzureBlobStoreSchema.SHARED_KEY);

        var secret = secretClient.setSecret(name, accountKey);

        return dataFactoryManager
                .linkedServices()
                .define(name)
                .withExistingFactory(factory.resourceGroupName(), factory.name())
                .withProperties(
                        new AzureStorageLinkedService()
                                .withConnectionString(format("DefaultEndpointsProtocol=https;AccountName=%s;", accountName))
                                .withAccountKey(
                                        new AzureKeyVaultSecretReference()
                                                .withSecretName(secret.getName())
                                                .withStore(new LinkedServiceReference()
                                                        .withReferenceName(keyVaultLinkedService)
                                                )))
                .create();
    }

    @SuppressWarnings("UnusedDeclaration")
    private enum DataFactoryPipelineRunStates {
        Queued(false, false, false),
        InProgress(false, false, false),
        Succeeded(true, true, false),
        Failed(true, false, true),
        Canceling(false, false, true),
        Cancelled(true, false, true);

        final boolean terminal;
        final boolean succeeded;
        final boolean failed;

        DataFactoryPipelineRunStates(boolean terminal, boolean succeeded, boolean failed) {
            this.terminal = terminal;
            this.succeeded = succeeded;
            this.failed = failed;
        }
    }
}
