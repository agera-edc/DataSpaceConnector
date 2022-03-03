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
package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory.pipeline;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.Context;
import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
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
import com.azure.resourcemanager.datafactory.models.PipelineElapsedTimeMetricPolicy;
import com.azure.resourcemanager.datafactory.models.PipelinePolicy;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateSharedKey;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

public class AzureDataFactoryTransferService implements TransferService {
    private final Monitor monitor;

    static String keyVaultLinkedService = propOrEnv("edc.datafactory.keyvault.linkedservicename", "AzureKeyVault");
    static String dataFactoryId = requiredPropOrEnv("edc.datafactory.resourceid");
    static String keyVaultId = requiredPropOrEnv("edc.datafactory.keyvault.resourceid");

    static AzureCliCredential credential = new AzureCliCredentialBuilder().build();
    static AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
    static DataFactoryManager dataFactoryManager = DataFactoryManager.authenticate(credential, profile);
    public static AzureResourceManager resourceManager = AzureResourceManager.authenticate(credential, profile)
            .withSubscription(requiredPropOrEnv("AZURE_SUBSCRIPTION_ID"));
    static GenericResource factory = resourceManager.genericResources().getById(dataFactoryId);
    static Vault vault = resourceManager.vaults().getById(keyVaultId);

    static Context context = Context.NONE;
    static SecretClient secretClient = new SecretClientBuilder()
            .vaultUrl(vault.vaultUri())
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();

    public AzureDataFactoryTransferService(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return
                AzureBlobStoreSchema.TYPE.equals(request.getSourceDataAddress().getType()) &&
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
        return VALID;
    }

    @Override
    public CompletableFuture<TransferResult> transfer(DataFlowRequest request) {

        String referencePipelineRunId = null;
        Boolean isRecovery = null;
        String startActivityName = null;
        Boolean startFromFailure = null;
        Map<String, Object> parameters = null;

        var baseName = "EDC-DPF-" + UUID.randomUUID();

        monitor.info("Creating ADF pipeline for " + baseName);

        DatasetResource src = createDataset(baseName + "src", request.getSourceDataAddress());
        DatasetResource dst = createDataset(baseName + "dst", request.getDestinationDataAddress());

        var pipeline =
                dataFactoryManager.pipelines()
                        .define(baseName)
                        .withExistingFactory(factory.resourceGroupName(), factory.name())
                        .withActivities(List.of(new CopyActivity()
                                .withName("CopyActivity")
                                .withInputs(List.of(new DatasetReference().withReferenceName(src.name())))
                                .withOutputs(List.of(new DatasetReference().withReferenceName(dst.name())))
                                .withSource(new BlobSource())
                                .withSink(new BlobSink())
                                .withValidateDataConsistency(true)
                                .withDataIntegrationUnits(32)))
                        .withPolicy(
                                new PipelinePolicy()
                                        .withElapsedTimeMetric(new PipelineElapsedTimeMetricPolicy().withDuration("0.00:10:00")))
                        .create();

        var createRunResponse =
                dataFactoryManager.pipelines()
                        .createRunWithResponse(
                                factory.resourceGroupName(),
                                factory.name(),
                                pipeline.name(),
                                referencePipelineRunId,
                                isRecovery,
                                startActivityName,
                                startFromFailure,
                                parameters,
                                context);
        var runId =
                createRunResponse.getValue().runId();

        monitor.info("Awaiting ADF pipeline completion for " + baseName);
        while (true) {
            var pipelineRun =
                    dataFactoryManager
                            .pipelineRuns()
                            .getWithResponse(factory.resourceGroupName(), factory.name(), runId, context);
            var runStatus = pipelineRun.getValue().status();
            String message = pipelineRun.getValue().message();
            monitor.info("ADF pipeline status is " + runStatus + " " + message + " for " + baseName);
            var runStatus2 = DataFactoryPipelineRunStates.valueOf(runStatus);
            switch (runStatus2) {
                case Queued:
                case InProgress:
                    break;
                case Succeeded:
                    return CompletableFuture.completedFuture(TransferResult.success());
                default:
                    return CompletableFuture.completedFuture(TransferResult.failure(ERROR_RETRY, message));
            }
        }

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
        String accountName = dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        String accountKey = dataAddress.getProperty(AzureBlobStoreSchema.SHARED_KEY);

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

    private enum DataFactoryPipelineRunStates {
        Queued, InProgress, Succeeded, Failed,
        Canceling, Cancelled
    }

    private static String requiredPropOrEnv(String s) {
        return Objects.requireNonNull(propOrEnv(s, null), s);
    }
}
