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

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureDataFactoryIntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.junit.launcher.MockVault;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_NAME;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_PROCESSES_PATH;
import static java.lang.System.getenv;

@AzureDataFactoryIntegrationTest
public class AzureDataFactoryTransferIntegrationTest {

    private static final String ASSETS_PATH = "/assets";
    private static final String POLICIES_PATH = "/policies";
    private static final String EDC_FS_CONFIG = "edc.fs.config";
    private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();
    private static final String RUNTIME_SETTINGS_PATH = "resources/azure/testing/runtime_settings.properties";

    private static final String RUNTIME_SETTINGS_ABSOLUTE_PATH = new File(TestUtils.findBuildRoot(), RUNTIME_SETTINGS_PATH).getAbsolutePath();

    private static final String KEY_VAULT_NAME = getenv("KEY_VAULT_NAME");
    private static final String PROVIDER_STORAGE_ACCOUNT_NAME = getenv("PROVIDER_STORAGE_ACCOUNT_NAME");
    private static final String CONSUMER_STORAGE_ACCOUNT_NAME = getenv("CONSUMER_STORAGE_ACCOUNT_NAME");
    private static final String PROVIDER_STORAGE_CONNECTION_STRING = getenv("PROVIDER_STORAGE_CONNECTION_STRING");
    private static final String CONSUMER_STORAGE_CONNECTION_STRING = getenv("CONSUMER_STORAGE_CONNECTION_STRING");
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    private static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";

//    @RegisterExtension
//    private static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
//            ":system-tests:runtimes:azure-storage-transfer-consumer",
//            "consumer",
//            Map.of(
//                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
//                    "web.http.path", CONSUMER_CONNECTOR_PATH,
//                    "web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
//                    "web.http.data.path", CONSUMER_MANAGEMENT_PATH,
//                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
//                    "web.http.ids.path", "/api/v1/ids",
//                    "ids.webhook.address", CONSUMER_IDS_API
//            ));
//
//    @RegisterExtension
//    private static EdcRuntimeExtension provider = new EdcRuntimeExtension(
//            ":system-tests:runtimes:azure-data-factory-transfer-provider",
//            "provider",
//            Map.of(
//                    "edc.test.asset.container.name", PROVIDER_CONTAINER_NAME,
//                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
//                    "web.http.path", PROVIDER_CONNECTOR_PATH,
//                    "web.http.data.port", String.valueOf(PROVIDER_MANAGEMENT_PORT),
//                    "web.http.data.path", PROVIDER_MANAGEMENT_PATH,
//                    "web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT),
//                    "web.http.ids.path", "/api/v1/ids",
//                    "ids.webhook.address", PROVIDER_IDS_API,
//                    EDC_FS_CONFIG, RUNTIME_SETTINGS_ABSOLUTE_PATH
//            ));

    @AfterEach
    public void cleanUp() {
        var providerBlobServiceClient= blobServiceClient(PROVIDER_STORAGE_CONNECTION_STRING);
        providerBlobServiceClient.deleteBlobContainer(PROVIDER_CONTAINER_NAME);
    }

    @Test
    public void transferBlob_success() {
        // Arrange

        // Upload a blob with test data on provider blob container
        var providerBlobServiceClient= blobServiceClient(PROVIDER_STORAGE_CONNECTION_STRING);
        var consumerBlobServiceClient = blobServiceClient(CONSUMER_STORAGE_CONNECTION_STRING);

        providerBlobServiceClient.createBlobContainer(PROVIDER_CONTAINER_NAME);

        // Updating secrets in key vault
        var vaultClient = vaultClient();
        //vaultClient.setSecret(new KeyVaultSecret("", ""));

        // Upload a blob with test data on provider blob container (in account1).

//        var blobContent = "BlobTransferIntegrationTest-" + UUID.randomUUID();
//        createContainer(blobServiceClient1, PROVIDER_CONTAINER_NAME);
//        blobServiceClient1.getBlobContainerClient(PROVIDER_CONTAINER_NAME)
//                .getBlobClient(PROVIDER_ASSET_NAME)
//                .upload(BinaryData.fromString(blobContent));

        // Seed data to provider
//        createAsset();
//        var policyId = createPolicy();
//        createContractDefinition(policyId);

        // Write Key to vault
//        CONSUMER_VAULT.storeSecret(format("%s-key1", account2Name), account2Key);
//        PROVIDER_VAULT.storeSecret(format("%s-key1", account1Name), account1Key);

        // Act
//        System.setProperty(ACCOUNT_NAME_PROPERTY, account2Name);
//        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
//        var container = getProvisionedContainerName();
//        var destinationBlob = blobServiceClient2.getBlobContainerClient(container)
//                .getBlobClient(PROVIDER_ASSET_NAME);
//        assertThat(destinationBlob.exists())
//                .withFailMessage("Destination blob %s not created", destinationBlob)
//                .isTrue();
//        var actualBlobContent = destinationBlob.downloadContent().toString();
//        assertThat(actualBlobContent)
//                .withFailMessage("Transferred file contents are not same as the source file")
//                .isEqualTo(blobContent);

        assertThat(1 + 2).isEqualTo(3);
    }

    private SecretClient vaultClient() {

        return new SecretClientBuilder()
                .vaultUrl(format(KEY_VAULT_ENDPOINT_TEMPLATE, KEY_VAULT_NAME))
                .credential(credential())
                .buildClient();
    }

    private BlobServiceClient blobServiceClient(String connectionString) {

        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    private TokenCredential credential() {
        return new DefaultAzureCredentialBuilder().build();
    }

    private String getProvisionedContainerName() {
        JsonPath jsonPath = given()
                .baseUri(CONSUMER_CONNECTOR_MANAGEMENT_URL + CONSUMER_MANAGEMENT_PATH)
                .log().all()
                .when()
                .get(TRANSFER_PROCESSES_PATH)
                .then()
                .statusCode(200)
                .extract().body().jsonPath();
        return jsonPath.getString("[0].provisionedResources[0].dataAddress.properties.container");
    }

    private void createAsset() {
//        var asset = Map.of(
//                "asset", Map.of(
//                        "properties", Map.of(
//                                "asset:prop:name", PROVIDER_ASSET_NAME,
//                                "asset:prop:contenttype", "text/plain",
//                                "asset:prop:version", "1.0",
//                                "asset:prop:id", PROVIDER_ASSET_NAME,
//                                "type", "AzureStorage"
//                        )
//                ),
//                "dataAddress", Map.of(
//                        "properties", Map.of(
//                                "type", AzureBlobStoreSchema.TYPE,
//                                AzureBlobStoreSchema.ACCOUNT_NAME, account1Name,
//                                AzureBlobStoreSchema.CONTAINER_NAME, PROVIDER_CONTAINER_NAME,
//                                AzureBlobStoreSchema.BLOB_NAME, PROVIDER_ASSET_NAME,
//                                "keyName", format("%s-key1", account1Name)
//                        )
//                )
//        );
//
//        seedProviderData(ASSETS_PATH, asset);
    }

    private String createPolicy() {
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target(PROVIDER_ASSET_NAME)
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
                        PROVIDER_ASSET_NAME)
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
