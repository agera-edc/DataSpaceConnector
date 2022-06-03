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
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

package org.eclipse.dataspaceconnector.extension.sample.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.common.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@EndToEndTest
public class FileTransferSampleTest {

    static final String INITIATE_CONTRACT_NEGOTIATION_URI = "http://localhost:9192/api/v1/data/contractnegotiations";
    static final String LOOK_UP_CONTRACT_AGREEMENT_URI = "http://localhost:9192/api/v1/data/contractnegotiations/{id}";
    static final String INITIATE_TRANSFER_PROCESS_URI = "http://localhost:9192/api/v1/data/transferprocess";
    static final String CONTRACT_OFFER_FILE_PATH = "samples/04.0-file-transfer/contractoffer.json";
    static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.0-file-transfer/consumer/config.properties";
    static final String PROVIDER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.0-file-transfer/provider/config.properties";
    static final String TRANSFER_FILE_PATH = "samples/04.0-file-transfer/filetransfer.json";
    // Reuse an already existing file for the test. Could be set to any other existing file in the repository.
    static final String SAMPLE_ASSET_FILE_PATH = TRANSFER_FILE_PATH;
    static final String DESTINATION_FILE_PATH = "samples/04.0-file-transfer/consumer/requested.test.txt";
    static final Duration TIMEOUT = Duration.ofSeconds(15);
    static final Duration POLL_INTERVAL = Duration.ofMillis(1000);
    static final String API_KEY_HEADER_KEY = "X-Api-Key";
    static final String API_KEY_HEADER_VALUE = "password";
    @RegisterExtension
    static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:provider",
            "provider",
            Map.of(
                    // Override 'edc.samples.04.asset.path' implicitly set via property 'edc.fs.config'.
                    "edc.samples.04.asset.path", new File(TestUtils.findBuildRoot(), SAMPLE_ASSET_FILE_PATH).getAbsolutePath(),
                    "edc.fs.config", new File(TestUtils.findBuildRoot(), PROVIDER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    // Starting EDC runtimes implicitly aligns to Run the sample / 1. Build and start the connectors.
    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:consumer",
            "consumer",
            Map.of(
                    "edc.fs.config", new File(TestUtils.findBuildRoot(), CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    final File destinationFile = new File(TestUtils.findBuildRoot(), FileTransferSampleTest.DESTINATION_FILE_PATH);
    String contractNegotiationId;
    String contractAgreementId;

    /**
     * Reads a properties file and returns a {@link Properties} instance for the given properties file.
     * @param fileName Path to a properties file.
     * @return A {@link Properties} instance for the given properties file.
     * @throws IOException Thrown if error while accessing the specified properties file.
     */
     static Properties readPropertiesFile(String fileName) throws IOException {
        try (var propertiesFileInputStream = new FileInputStream(fileName)) {
            Properties prop = new Properties();
            prop.load(propertiesFileInputStream);
            return prop;
        }
    }

    /**
     * Run all sample steps in one single test.
     * Note: Sample steps cannot be separated into single tests because {@link EdcRuntimeExtension}
     * runs before each single test.
     */
    @Test
    void runSampleSteps() throws IOException {
        assertTestPrerequisites();

        assertConfigPropertiesUniquePorts();
        assertInitiateContractNegotiation();
        assertLookUpContractAgreementId();
        assertRequestFile();
        assertWaitForDestinationFileExistence();

        cleanTemporaryTestFiles();
    }

    /**
     * Assert that prerequisites are fulfilled before running the test.
     * This assertion checks only whether the file to be copied is not existing already.
     */
    void assertTestPrerequisites() {
        var transferredFile = new File(TestUtils.findBuildRoot(), DESTINATION_FILE_PATH);

        Assertions.assertThat(transferredFile.exists()).isFalse();
    }

    /**
     * Remove files created while running the tests.
     * The copied file will be deleted.
     */
    void cleanTemporaryTestFiles() {
        var transferredFile = new File(TestUtils.findBuildRoot(), DESTINATION_FILE_PATH);

        if (transferredFile.exists()) {
            transferredFile.delete();
        }
    }

    /**
     * Assert that the file to be copied exists at the expected location.
     * This method waits a duration which is defined in {@link FileTransferSampleTest#TIMEOUT}.
     */
    void assertWaitForDestinationFileExistence() {
        var expectedFile = new File(TestUtils.findBuildRoot(), DESTINATION_FILE_PATH);

        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() ->
                Assertions.assertThat(expectedFile.exists()).isTrue()
        );
    }

    /**
     * Assert that a POST request to initiate a contract negotiation is successful.
     * This method corresponds to the command in the sample: curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/contractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"
     */
    void assertInitiateContractNegotiation() {
        JsonPath jsonPath = RestAssured
            .given()
                .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .body(new File(TestUtils.findBuildRoot(), CONTRACT_OFFER_FILE_PATH))
                .log().all()
            .when()
                .post(INITIATE_CONTRACT_NEGOTIATION_URI)
            .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .jsonPath();

        contractNegotiationId = jsonPath.get("id");
    }

    /**
     * Assert that a GET request to look up a contract agreement is successful.
     * This method corresponds to the command in the sample: curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/data/contractnegotiations/{UUID}"
     */
    void assertLookUpContractAgreementId() {
        var localContractAgreementId = new AtomicReference<String>();

        // Wait for transfer to be completed.
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
                    var result = RestAssured
                        .given()
                            .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                            .log().all()
                        .when()
                            .get(LOOK_UP_CONTRACT_AGREEMENT_URI, contractNegotiationId)
                        .then()
                            .statusCode(HttpStatus.SC_OK)
                            .body("state", equalTo("CONFIRMED"))
                            .extract().body().jsonPath().getString("contractAgreementId");

                    localContractAgreementId.set(result);

                }
        );

        contractAgreementId = localContractAgreementId.get();
    }

    /**
     * Assert that a POST request to initiate transfer process is successful.
     * This methods corresponds to the command in the sample: curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/filetransfer.json "http://localhost:9192/api/v1/data/transferprocess"
     * @throws IOException Thrown if there was an error accessing the transfer request file defined in {@link FileTransferSampleTest#TRANSFER_FILE_PATH}.
     */
    void assertRequestFile() throws IOException {
        File transferJsonFile = new File(TestUtils.findBuildRoot(), TRANSFER_FILE_PATH);
        DataRequest sampleDataRequest = readAndUpdateTransferJsonFile(transferJsonFile, contractAgreementId);

        JsonPath jsonPath = RestAssured
            .given()
                .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .body(sampleDataRequest)
                .log().all()
            .when()
                .post(INITIATE_TRANSFER_PROCESS_URI)
            .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .jsonPath();

        String transferProcessId = jsonPath.get("id");

        Assertions.assertThat(transferProcessId).isNotEmpty();
    }

    /**
     * Reads a transfer request file with changed value for contract agreement ID and file destination path.
     * @param transferJsonFile A {@link File} instance pointing to a JSON transfer request file.
     * @param contractAgreementId This string containing a UUID will be used as value for the contract agreement ID.
     * @return An instance of {@link DataRequest} with changed values for contract agreement ID and file destination path.
     * @throws IOException Thrown if there was an error accessing the file given in transferJsonFile.
     */
    DataRequest readAndUpdateTransferJsonFile(File transferJsonFile, String contractAgreementId) throws IOException {
        // create object mapper instance
        ObjectMapper mapper = new ObjectMapper();

        // convert JSON file to map
        DataRequest sampleDataRequest = mapper.readValue(transferJsonFile, DataRequest.class);

        DataAddress newDataDestination = sampleDataRequest.getDataDestination().toBuilder().property("path", destinationFile.getAbsolutePath()).build();
        DataRequest newSampleDataRequest = sampleDataRequest.toBuilder().contractId(contractAgreementId).dataDestination(newDataDestination).build();

        return newSampleDataRequest;
    }

    /**
     * Assert that the properties.config files are properly configured and all ports uniquely used.
     * Relates to sample guidance section: Create the connectors.
     */
    void assertConfigPropertiesUniquePorts() {
        String pathConsumerConfigProperties = new File(TestUtils.findBuildRoot(), CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath();
        String pathProviderConfigProperties = new File(TestUtils.findBuildRoot(), PROVIDER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath();

        Properties consumerProperties = null;
        Properties providerProperties = null;

        try {
            consumerProperties = readPropertiesFile(pathConsumerConfigProperties);
            providerProperties = readPropertiesFile(pathProviderConfigProperties);
        } catch (IOException e) {
            Assertions.fail("Could not read properties files.");
        }

        List<String> portPropertyNames = List.of(
                "web.http.port",
                "web.http.data.port",
                "web.http.ids.port"
        );

        HashMap<Integer, String> configuredPorts = new HashMap<>();

        for (var portPropertyName : portPropertyNames) {
            assertPortDefinedOnce(consumerProperties, portPropertyName, configuredPorts, "consumer");

            assertPortDefinedOnce(providerProperties, portPropertyName, configuredPorts, "provider");
        }
    }

    /**
     * Helper method to assert that a given port number is existing only once in the given HashMap configuredPorts.
     * If port number was not already defined it will be added to the HashMap configuredPorts with the given participant and property name.
     */
    void assertPortDefinedOnce(Properties properties, String portPropertyName, HashMap<Integer, String> configuredPorts, String participantName) {

        String propertyValue = properties.getProperty(portPropertyName);

        if (propertyValue == null) return;

        int portNumber = Integer.parseInt(propertyValue);

        if (configuredPorts.containsKey(portNumber)) {
            Assertions.fail(String.format("Port number '%d' defined multiple times.", portNumber));
        }

        configuredPorts.put(portNumber, String.format("%s/%s", participantName, portPropertyName));
    }
}
