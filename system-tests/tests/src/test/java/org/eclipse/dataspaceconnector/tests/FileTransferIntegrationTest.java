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

package org.eclipse.dataspaceconnector.tests;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import io.gatling.javaapi.core.Simulation;
import net.catenax.prs.systemtest.PerformanceTestsRunner;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.tempDirectory;
import static org.eclipse.dataspaceconnector.tests.FileTransferTestUtils.PROVIDER_ASSET_NAME;


/**
 * System Test for Sample 04.0-file-transfer
 */
public class FileTransferIntegrationTest {
    public static final String PROVIDER_ASSET_PATH = format("%s/%s.txt", tempDirectory(), PROVIDER_ASSET_NAME);

    public static final String CONSUMER_ASSET_PATH = tempDirectory();
    public static final int CONSUMER_CONNECTOR_PORT = getFreePort();
    public static final String CONSUMER_CONNECTOR_HOST = "http://localhost:" + CONSUMER_CONNECTOR_PORT;

    public static final int PROVIDER_CONNECTOR_PORT = getFreePort();
    public static final String PROVIDER_CONNECTOR_HOST = "http://localhost:" + PROVIDER_CONNECTOR_PORT;

    public static final String API_KEY_CONTROL_AUTH = "password";

    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:file-transfer-consumer",
            "consumer",
            Map.of(
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "edc.api.control.auth.apikey.value", API_KEY_CONTROL_AUTH,
                    "ids.webhook.address", CONSUMER_CONNECTOR_HOST));

    @RegisterExtension
    static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:file-transfer-provider",
            "provider",
            Map.of(
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "edc.test.asset.path", PROVIDER_ASSET_PATH,
                    "ids.webhook.address", PROVIDER_CONNECTOR_HOST));

    @Test
    public void transferFile_success() throws Exception {
        /*
        // Arrange
        // Create a file with test data on provider file system.
        var fileContent = "FileTransfer-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));

        // Act
        var client = new FileTransferTestUtils();
        client.setConsumerUrl(CONSUMER_CONNECTOR_HOST);
        client.setProviderUrl(PROVIDER_CONNECTOR_HOST);
        client.setDestinationPath(CONSUMER_ASSET_PATH);
        client.setApiKey(API_KEY_CONTROL_AUTH);

        var contractAgreementId = client.negotiateContractAgreement();
        client.performFileTransfer(contractAgreementId);

        // Assert
        var copiedFilePath = Path.of(format(CONSUMER_ASSET_PATH + "/%s.txt", PROVIDER_ASSET_NAME));
        assertThat(copiedFilePath)
                .withFailMessage("Destination file %s not created", copiedFilePath)
                .exists();
        var actualFileContent = Files.readString(copiedFilePath);
        assertThat(actualFileContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(fileContent);

         */
        runGatling(PerformanceTestsRunner.class);
    }

    static void runGatling(Class<? extends Simulation> simulation) {
        var props = new GatlingPropertiesBuilder();
        props.simulationClass(simulation.getCanonicalName());
        props.resultsDirectory("target/gatling");
        Gatling.fromMap(props.build());
    }
}
