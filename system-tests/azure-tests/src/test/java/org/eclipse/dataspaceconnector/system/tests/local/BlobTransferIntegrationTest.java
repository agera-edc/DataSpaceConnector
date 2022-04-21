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

import org.eclipse.dataspaceconnector.common.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.common.annotations.PerformanceTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.junit.launcher.MockVault;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferLocalSimulation.CONSUMER_ASSET_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferLocalSimulation.PROVIDER_ASSET_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;

@EndToEndTest
@PerformanceTest
public class BlobTransferIntegrationTest {

    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.of(
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "web.http.path", CONSUMER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
                    "web.http.data.path", CONSUMER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
                    "web.http.ids.path", "/api/v1/ids",
                    "ids.webhook.address", CONSUMER_IDS_API));

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-provider",
            "provider",
            Map.of(
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "web.http.path", PROVIDER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(PROVIDER_MANAGEMENT_PORT),
                    "web.http.data.path", PROVIDER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT),
                    "web.http.ids.path", "/api/v1/ids",
                    "ids.webhook.address", PROVIDER_IDS_API));

    static Vault consumerVault = new MockVault();
    static Vault providerVault = new MockVault();

    @BeforeAll
    static void beforeAll() {
        consumer.registerServiceMock(Vault.class, consumerVault);
        provider.registerServiceMock(Vault.class, providerVault);
    }

    @Test
    public void transferFile_success() throws Exception {
        // Arrange
        // Create a file with test data on provider file system.
        var fileContent = "FileTransfer-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));
        // Write Key to vault
//        consumerVault.storeSecret();
//        providerVault.storeSecret();

        // Act
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var copiedFilePath = Path.of(CONSUMER_ASSET_PATH);
        assertThat(copiedFilePath)
                .withFailMessage("Destination file %s not created", copiedFilePath)
                .exists();
        var actualFileContent = Files.readString(copiedFilePath);
        assertThat(actualFileContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(fileContent);
    }
}
