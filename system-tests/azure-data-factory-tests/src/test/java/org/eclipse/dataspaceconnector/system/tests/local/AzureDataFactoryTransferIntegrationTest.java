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
import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.restassured.path.json.JsonPath;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY;
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
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_NAME;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_PROCESSES_PATH;

@AzureDataFactoryIntegrationTest
public class AzureDataFactoryTransferIntegrationTest {

    private static final String ASSETS_PATH = "/assets";
    private static final String POLICIES_PATH = "/policies";
    private static final String EDC_FS_CONFIG = "edc.fs.config";
    private static final String EDC_VAULT_NAME = "edc.vault.name";
    private static final String EDC_VAULT_CLIENT_ID = "edc.vault.clientid";
    private static final String EDC_VAULT_TENANT_ID = "edc.vault.tenantid";
    private static final String EDC_VAULT_CLIENT_SECRET = "edc.vault.clientsecret";
    private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();
    private static final String RUNTIME_SETTINGS_PATH = "resources/azure/testing/runtime_settings.properties";

    private static final String RUNTIME_SETTINGS_ABSOLUTE_PATH = new File(TestUtils.findBuildRoot(), RUNTIME_SETTINGS_PATH).getAbsolutePath();

    private static final String KEY_VAULT_NAME = getenv("KEY_VAULT_NAME");
    private static final String TENANT_ID = getenv("TENANT_ID");
    private static final String SP_CLIENT_ID = getenv("SP_CLIENT_ID");
    private static final String SP_CLIENT_SECRET = getenv("SP_CLIENT_SECRET");
    private static final String PROVIDER_STORAGE_ACCOUNT_NAME = getenv("PROVIDER_STORAGE_ACCOUNT_NAME");
    private static final String PROVIDER_STORAGE_ACCOUNT_KEY = getenv("PROVIDER_STORAGE_ACCOUNT_KEY");
    private static final String PROVIDER_STORAGE_ACCOUNT_CONN_STRING = getenv("PROVIDER_STORAGE_ACCOUNT_CONN_STRING");
    private static final String CONSUMER_STORAGE_ACCOUNT_NAME = getenv("CONSUMER_STORAGE_ACCOUNT_NAME");
    private static final String CONSUMER_STORAGE_ACCOUNT_KEY = getenv("CONSUMER_STORAGE_ACCOUNT_KEY");
    private static final String CONSUMER_STORAGE_ACCOUNT_CONN_STRING = getenv("CONSUMER_STORAGE_ACCOUNT_CONN_STRING");
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    private static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";

    @RegisterExtension
    private static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.ofEntries(
                    Map.entry("web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", CONSUMER_CONNECTOR_PATH),
                    Map.entry("web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT)),
                    Map.entry("web.http.data.path", CONSUMER_MANAGEMENT_PATH),
                    Map.entry("web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT)),
                    Map.entry("web.http.ids.path", "/api/v1/ids"),
                    Map.entry("ids.webhook.address", CONSUMER_IDS_API),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(EDC_VAULT_CLIENT_ID, SP_CLIENT_ID),
                    Map.entry(EDC_VAULT_TENANT_ID, TENANT_ID),
                    Map.entry(EDC_VAULT_CLIENT_SECRET, SP_CLIENT_SECRET)
            )
    );

    @RegisterExtension
    private static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-data-factory-transfer-provider",
            "provider",
            Map.ofEntries(
                    Map.entry("edc.test.asset.container.name", PROVIDER_CONTAINER_NAME),
                    Map.entry("web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", PROVIDER_CONNECTOR_PATH),
                    Map.entry("web.http.data.port", String.valueOf(PROVIDER_MANAGEMENT_PORT)),
                    Map.entry("web.http.data.path", PROVIDER_MANAGEMENT_PATH),
                    Map.entry("web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT)),
                    Map.entry("web.http.ids.path", "/api/v1/ids"),
                    Map.entry("ids.webhook.address", PROVIDER_IDS_API),
                    Map.entry(EDC_FS_CONFIG, RUNTIME_SETTINGS_ABSOLUTE_PATH),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(EDC_VAULT_CLIENT_ID, SP_CLIENT_ID),
                    Map.entry(EDC_VAULT_TENANT_ID, TENANT_ID),
                    Map.entry(EDC_VAULT_CLIENT_SECRET, SP_CLIENT_SECRET)
            )
    );

    @AfterEach
    public void cleanUp() {
        var providerBlobServiceClient = blobServiceClient(PROVIDER_STORAGE_ACCOUNT_CONN_STRING);
        providerBlobServiceClient.deleteBlobContainer(PROVIDER_CONTAINER_NAME);
    }

    @Test
    public void transferBlob_success() {
        // Arrange

        // Upload a blob with test data on provider blob container
        var providerBlobServiceClient = blobServiceClient(PROVIDER_STORAGE_ACCOUNT_CONN_STRING);
        var consumerBlobServiceClient = blobServiceClient(CONSUMER_STORAGE_ACCOUNT_CONN_STRING);
        var blobContent = "AzureDataFactoryTransferIntegrationTest-" + UUID.randomUUID();

        var providerBlobServiceClientBlobContainer = providerBlobServiceClient.createBlobContainer(PROVIDER_CONTAINER_NAME);
        providerBlobServiceClientBlobContainer
                .getBlobClient(PROVIDER_ASSET_NAME)
                .upload(BinaryData.fromString(blobContent), true);

        // Updating secrets in key vault
        var vaultClient = vaultClient();
        vaultClient.setSecret(new KeyVaultSecret(PROVIDER_STORAGE_ACCOUNT_NAME + "-key1", PROVIDER_STORAGE_ACCOUNT_KEY));
        vaultClient.setSecret(new KeyVaultSecret(CONSUMER_STORAGE_ACCOUNT_NAME + "-key1", CONSUMER_STORAGE_ACCOUNT_KEY));

        // Seed data to provider
        createAsset();
        var policyId = createPolicy();
        createContractDefinition(policyId);

        // Act
        System.setProperty(BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY, CONSUMER_STORAGE_ACCOUNT_NAME);
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var container = getProvisionedContainerName();
        var destinationBlob = consumerBlobServiceClient.getBlobContainerClient(container)
                .getBlobClient(PROVIDER_ASSET_NAME);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob)
                .isTrue();
        var actualBlobContent = destinationBlob.downloadContent().toString();
        assertThat(actualBlobContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(blobContent);

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
        var asset = Map.of(
                "asset", Map.of(
                        "properties", Map.of(
                                "asset:prop:name", PROVIDER_ASSET_NAME,
                                "asset:prop:contenttype", "text/plain",
                                "asset:prop:version", "1.0",
                                "asset:prop:id", PROVIDER_ASSET_NAME,
                                "type", "AzureStorage"
                        )
                ),
                "dataAddress", Map.of(
                        "properties", Map.of(
                                "type", AzureBlobStoreSchema.TYPE,
                                AzureBlobStoreSchema.ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_NAME,
                                AzureBlobStoreSchema.CONTAINER_NAME, PROVIDER_CONTAINER_NAME,
                                AzureBlobStoreSchema.BLOB_NAME, PROVIDER_ASSET_NAME,
                                "keyName", format("%s-key1", PROVIDER_STORAGE_ACCOUNT_NAME)
                        )
                )
        );

        seedProviderData(ASSETS_PATH, asset);
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
