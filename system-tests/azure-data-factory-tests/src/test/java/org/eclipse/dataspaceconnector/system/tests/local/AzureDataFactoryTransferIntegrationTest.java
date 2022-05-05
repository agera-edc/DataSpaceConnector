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

package org.eclipse.dataspaceconnector.system.tests.local;

import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureDataFactoryIntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.getenv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.IDS_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_ID;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_PROCESSES_PATH;

@AzureDataFactoryIntegrationTest
public class AzureDataFactoryTransferIntegrationTest {

    private static final List<Runnable> containerCleanup = new ArrayList<>();
    private static final String ASSETS_PATH = "/assets";
    private static final String POLICIES_PATH = "/policies";
    private static final String EDC_FS_CONFIG = "edc.fs.config";
    private static final String EDC_VAULT_NAME = "edc.vault.name";
    private static final String EDC_VAULT_CLIENT_ID = "edc.vault.clientid";
    private static final String EDC_VAULT_TENANT_ID = "edc.vault.tenantid";
    private static final String EDC_VAULT_CLIENT_SECRET = "edc.vault.clientsecret";
    private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();
    private static final String KEY_VAULT_NAME = runtimeSettingsProperties().getProperty("test.key.vault.name");
    private static final String AZURE_TENANT_ID = getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_ID = getenv("AZURE_CLIENT_ID");
    private static final String AZURE_CLIENT_SECRET = getenv("AZURE_CLIENT_SECRET");
    private static final String PROVIDER_STORAGE_ACCOUNT_NAME = runtimeSettingsProperties().getProperty("test.provider.storage.name");
    private static final String CONSUMER_STORAGE_ACCOUNT_NAME = runtimeSettingsProperties().getProperty("test.consumer.storage.name");
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    private static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";

    @RegisterExtension
    private static final EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(CONSUMER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", CONSUMER_CONNECTOR_PATH),
                    Map.entry("web.http.data.port", valueOf(CONSUMER_MANAGEMENT_PORT)),
                    Map.entry("web.http.data.path", CONSUMER_MANAGEMENT_PATH),
                    Map.entry("web.http.ids.port", valueOf(CONSUMER_IDS_API_PORT)),
                    Map.entry("web.http.ids.path", IDS_PATH),
                    Map.entry("ids.webhook.address", CONSUMER_IDS_API),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(EDC_VAULT_CLIENT_ID, AZURE_CLIENT_ID),
                    Map.entry(EDC_VAULT_TENANT_ID, AZURE_TENANT_ID),
                    Map.entry(EDC_VAULT_CLIENT_SECRET, AZURE_CLIENT_SECRET)
            )
    );

    @RegisterExtension
    private static final EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-data-factory-transfer-provider",
            "provider",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(PROVIDER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", PROVIDER_CONNECTOR_PATH),
                    Map.entry("web.http.data.port", valueOf(PROVIDER_MANAGEMENT_PORT)),
                    Map.entry("web.http.data.path", PROVIDER_MANAGEMENT_PATH),
                    Map.entry("web.http.ids.port", valueOf(PROVIDER_IDS_API_PORT)),
                    Map.entry("web.http.ids.path", IDS_PATH),
                    Map.entry("ids.webhook.address", PROVIDER_IDS_API),
                    Map.entry(EDC_FS_CONFIG, runtimeSettingsPath()),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(EDC_VAULT_CLIENT_ID, AZURE_CLIENT_ID),
                    Map.entry(EDC_VAULT_TENANT_ID, AZURE_TENANT_ID),
                    Map.entry(EDC_VAULT_CLIENT_SECRET, AZURE_CLIENT_SECRET)
            )
    );

    @BeforeAll
    static void beforeAll() throws FileNotFoundException {
        var file = new File(runtimeSettingsPath());
        if (!file.exists()) {
            throw new FileNotFoundException("Runtime settings file not found");
        }
    }

    @AfterAll
    static void cleanUp() {
        containerCleanup.parallelStream().forEach(Runnable::run);
    }

    @Test
    public void transferBlob_success() {
        // Arrange

        // Upload a blob with test data on provider blob container
        var providerBlobServiceClient = getBlobServiceClient(PROVIDER_STORAGE_ACCOUNT_NAME);
        var consumerBlobServiceClient = getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME);
        var blobContent = "AzureDataFactoryTransferIntegrationTest-" + UUID.randomUUID();

        providerBlobServiceClient
                .createBlobContainer(PROVIDER_CONTAINER_NAME)
                .getBlobClient(PROVIDER_ASSET_ID)
                .upload(BinaryData.fromString(blobContent), true);
        // Add for cleanup
        containerCleanup.add(() -> providerBlobServiceClient.deleteBlobContainer(PROVIDER_CONTAINER_NAME));

        // Seed data to provider
        createAsset();
        var policyId = createPolicy();
        createContractDefinition(policyId);

        // Act
        System.setProperty(BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY, CONSUMER_STORAGE_ACCOUNT_NAME);
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var provisionedContainerName = getProvisionedContainerName();
        // Add for cleanup
        containerCleanup.add(() -> consumerBlobServiceClient.deleteBlobContainer(provisionedContainerName));

        var destinationBlob = consumerBlobServiceClient.getBlobContainerClient(provisionedContainerName)
                .getBlobClient(PROVIDER_ASSET_ID);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob)
                .isTrue();
        var actualBlobContent = destinationBlob.downloadContent().toString();
        assertThat(actualBlobContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(blobContent);

    }

    @NotNull
    private static BlobServiceClient getBlobServiceClient(String accountName) {
        var credential = new DefaultAzureCredentialBuilder().build();
        var vault = new SecretClientBuilder()
                .vaultUrl(format(KEY_VAULT_ENDPOINT_TEMPLATE, KEY_VAULT_NAME))
                .credential(credential)
                .buildClient();
        var accountKey = vault.getSecret(accountName + "-key1");
        return new BlobServiceClientBuilder()
                .endpoint(format(BLOB_STORE_ENDPOINT_TEMPLATE, accountName))
                .credential(new StorageSharedKeyCredential(accountName, accountKey.getValue()))
                .buildClient();
    }

    @NotNull
    private String getProvisionedContainerName() {
        return given()
                .baseUri(CONSUMER_CONNECTOR_MANAGEMENT_URL + CONSUMER_MANAGEMENT_PATH)
                .when()
                .get(TRANSFER_PROCESSES_PATH)
                .then()
                .statusCode(200)
                .extract().body()
                .jsonPath().getString("[0].dataDestination.properties.container");
    }

    @NotNull
    private static String runtimeSettingsPath() {
        return new File(TestUtils.findBuildRoot(), "resources/azure/testing/runtime_settings.properties").getAbsolutePath();
    }

    @NotNull
    private static Properties runtimeSettingsProperties() {
        try (InputStream input = new FileInputStream(runtimeSettingsPath())) {
            Properties prop = new Properties();
            prop.load(input);

            return prop;
        } catch (IOException e) {
            throw new RuntimeException("Error in loading runtime settings properties", e);
        }
    }

    private void createAsset() {
        var asset = Map.of(
                "asset", Map.of(
                        "properties", Map.of(
                                "asset:prop:name", PROVIDER_ASSET_ID,
                                "asset:prop:contenttype", "text/plain",
                                "asset:prop:version", "1.0",
                                "asset:prop:id", PROVIDER_ASSET_ID,
                                "type", "AzureStorage"
                        )
                ),
                "dataAddress", Map.of(
                        "properties", Map.of(
                                "type", AzureBlobStoreSchema.TYPE,
                                AzureBlobStoreSchema.ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_NAME,
                                AzureBlobStoreSchema.CONTAINER_NAME, PROVIDER_CONTAINER_NAME,
                                AzureBlobStoreSchema.BLOB_NAME, PROVIDER_ASSET_ID,
                                "keyName", format("%s-key1", PROVIDER_STORAGE_ACCOUNT_NAME)
                        )
                )
        );

        seedProviderData(ASSETS_PATH, asset);
    }

    private String createPolicy() {
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target(PROVIDER_ASSET_ID)
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();

        seedProviderData(POLICIES_PATH, policy);

        return policy.getUid();
    }

    private void createContractDefinition(String policyId) {

        var criteria = AssetSelectorExpression.Builder.newInstance()
                .constraint("asset:prop:id",
                        "=",
                        PROVIDER_ASSET_ID)
                .build();

        var contractDefinition = Map.of(
                "id", "1",
                "accessPolicyId", policyId,
                "contractPolicyId", policyId,
                "criteria", criteria.getCriteria()
        );

        seedProviderData(CONTRACT_DEFINITIONS_PATH, contractDefinition);
    }

    private void seedProviderData(String path, Object requestBody) {
        givenProviderBaseRequest()
                .log().all()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(204);
    }

    private RequestSpecification givenProviderBaseRequest() {
        return given()
                .baseUri(PROVIDER_CONNECTOR_MANAGEMENT_URL + PROVIDER_MANAGEMENT_PATH);
    }
}
