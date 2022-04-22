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
 *       ZF Friedrichshafen AG - add management api configurations
 *       Fraunhofer Institute for Software and Systems Engineering - added IDS API context
 *
 */

package org.eclipse.dataspaceconnector.system.tests.local;

import org.apache.commons.lang3.StringUtils;
import com.azure.core.util.BinaryData;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.dataspaceconnector.common.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.common.annotations.PerformanceTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.junit.launcher.MockVault;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.NullVaultExtension;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY;
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

@EndToEndTest
@PerformanceTest
public class BlobTransferIntegrationTest extends AbstractAzureBlobTest {
    private static final Vault CONSUMER_VAULT = new MockVault();
    private static final Vault PROVIDER_VAULT = new MockVault();
    private static final String ASSETS_PATH = "/assets";
    private static final String SOURCE_BLOB_CONTAINER = "src-container";

    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.of(
                    "edc.blobstore.endpoint", "http://127.0.0.1:10000/account2",
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "web.http.path", CONSUMER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
                    "web.http.data.path", CONSUMER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
                    "web.http.ids.path", "/api/v1/ids",
                    "ids.webhook.address", CONSUMER_IDS_API));

    protected static String providerContainerName = UUID.randomUUID().toString();
    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-provider",
            "provider",
            Map.of(
                    "edc.blobstore.endpoint.template", "http://127.0.0.1:10000/%s",
                    "edc.test.asset.container.name", providerContainerName,
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "web.http.path", PROVIDER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(PROVIDER_MANAGEMENT_PORT),
                    "web.http.data.path", PROVIDER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT),
                    "web.http.ids.path", "/api/v1/ids",
                    "ids.webhook.address", PROVIDER_IDS_API));


    @BeforeAll
    static void beforeAll() {
        setUpMockVault(consumer, CONSUMER_VAULT);
        setUpMockVault(provider, PROVIDER_VAULT);
    }

    private static void setUpMockVault(EdcRuntimeExtension consumer, Vault vault) {
        consumer.registerServiceMock(Vault.class, vault);
        consumer.registerServiceMock(PrivateKeyResolver.class, new NullVaultExtension().getPrivateKeyResolver());
        consumer.registerServiceMock(CertificateResolver.class, new NullVaultExtension().getCertificateResolver());
    }

    @Test
    public void transferBlob_success() {
        // Arrange
        // Upload a blob with test data on provider blob container (in account1).
        var blobContent = "BlobTransferIntegrationTest-" + UUID.randomUUID();
        createContainer(blobServiceClient1, providerContainerName);
        blobServiceClient1.getBlobContainerClient(providerContainerName)
                .getBlobClient(PROVIDER_ASSET_NAME)
                .upload(BinaryData.fromString(blobContent));

        // Write Key to vault
        CONSUMER_VAULT.storeSecret(account2Name + "-key1", account2Key);
        PROVIDER_VAULT.storeSecret(account1Name + "-key1", account1Key);

        // Act
        System.setProperty(ACCOUNT_NAME_PROPERTY, account2Name);
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var destinationBlob = blobServiceClient2.getBlobContainerClient("TODO-replace-with-resultContainerName")
                .getBlobClient(PROVIDER_ASSET_NAME);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob)
                .isTrue();
        var actualBlobContent = destinationBlob.downloadContent().toString();
        assertThat(actualBlobContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(blobContent);
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
                                "type", "AzureStorage",
                                "account", account1Name,
                                "container", SOURCE_BLOB_CONTAINER,
                                "blobname", format("%s.txt", PROVIDER_ASSET_NAME),
                                "keyName", format("%s-key1", account1Name)
                        )
                )
        );

        seedProviderData(ASSETS_PATH, asset);
    }

    private void seedProviderData(String path, Object requestBody) {
        givenProviderBaseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(204);
    }

    private RequestSpecification givenProviderBaseRequest() {
        return given()
                .baseUri(PROVIDER_CONNECTOR_MANAGEMENT_URL)
                .basePath(PROVIDER_MANAGEMENT_PATH)
                .when();
    }
}
