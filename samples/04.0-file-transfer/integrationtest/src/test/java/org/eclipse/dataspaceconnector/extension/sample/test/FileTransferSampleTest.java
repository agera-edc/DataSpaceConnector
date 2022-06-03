package org.eclipse.dataspaceconnector.extension.sample.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.assertj.core.api.Assertions;
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

public class FileTransferSampleTest {

    public static final String INITIATE_CONTRACT_NEGOTIATION_URI = "http://localhost:9192/api/v1/data/contractnegotiations";
    private static final String LOOK_UP_CONTRACT_AGREEMENT_URI = "http://localhost:9192/api/v1/data/contractnegotiations/{id}";
    public static final String INITIATE_TRANSFER_PROCESS_URI = "http://localhost:9192/api/v1/data/transferprocess";
    public static final String CONTRACT_OFFER_FILE_PATH = "samples/04.0-file-transfer/contractoffer.json";
    public static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.0-file-transfer/consumer/config.properties";
    public static final String PROVIDER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.0-file-transfer/provider/config.properties";
    private static final String TRANSFER_FILE_PATH = "samples/04.0-file-transfer/filetransfer.json";
    // Reuse an already existing file for the test. Could be set to any other existing file in the repository.
    private static final String SAMPLE_ASSET_FILE_PATH = TRANSFER_FILE_PATH;
    private static final String DESTINATION_FILE_PATH = "samples/04.0-file-transfer/consumer/requested.test.txt";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final String API_KEY_HEADER_KEY = "X-Api-Key";
    private static final String API_KEY_HEADER_VALUE = "password";
    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
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
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:consumer",
            "consumer",
            Map.of(
                    "edc.fs.config", new File(TestUtils.findBuildRoot(), CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    private final File destinationFile = new File(TestUtils.findBuildRoot(), FileTransferSampleTest.DESTINATION_FILE_PATH);
    private String contractNegotiationId;
    private String contractAgreementId;

    private static Properties readPropertiesFile(String fileName) throws IOException {
        FileInputStream fis = null;
        Properties prop = new Properties();

        try {
            fis = new FileInputStream(fileName);
            prop.load(fis);

        } finally {
            if (fis != null) fis.close();
        }

        return prop;
    }

    /**
     * Run all sample steps in one single test.
     * Note: Sample steps cannot be separated into single tests because {@link EdcRuntimeExtension} runs before each single test.
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

    private void assertTestPrerequisites() {
        var transferredFile = new File(TestUtils.findBuildRoot(), DESTINATION_FILE_PATH);

        Assertions.assertThat(transferredFile.exists()).isFalse();
    }

    private void cleanTemporaryTestFiles() {
        var transferredFile = new File(TestUtils.findBuildRoot(), DESTINATION_FILE_PATH);

        if (transferredFile.exists()) {
            transferredFile.delete();
        }
    }

    void assertWaitForDestinationFileExistence() {
        var expectedFile = new File(TestUtils.findBuildRoot(), DESTINATION_FILE_PATH);

        await().atMost(TIMEOUT).pollInterval(1000, MILLISECONDS).untilAsserted(() ->
                Assertions.assertThat(expectedFile.exists()).isTrue()
        );
    }

    void assertInitiateContractNegotiation() {
        // curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/contractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"

        JsonPath jsonPath = RestAssured
                .given()
                .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .body(new File(TestUtils.findBuildRoot(), CONTRACT_OFFER_FILE_PATH))
                .log().all()
                .when()
                .post(INITIATE_CONTRACT_NEGOTIATION_URI)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        contractNegotiationId = jsonPath.get("id");
    }

    void assertLookUpContractAgreementId() {
        // curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/data/contractnegotiations/{UUID}"

        var localContractAgreementId = new AtomicReference<String>();

        // Wait for transfer to be completed.
        await().atMost(TIMEOUT).pollInterval(1000, MILLISECONDS).untilAsserted(() -> {
                    var result = RestAssured
                            .given()
                            .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                            .log().all()
                            .when()
                            .get(LOOK_UP_CONTRACT_AGREEMENT_URI, contractNegotiationId)
                            .then()
                            .statusCode(200)
                            .body("state", equalTo("CONFIRMED"))
                            .extract().body().jsonPath().getString("contractAgreementId");

                    localContractAgreementId.set(result);

                }
        );

        contractAgreementId = localContractAgreementId.get();
    }

    void assertRequestFile() throws IOException {
        // curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/filetransfer.json "http://localhost:9192/api/v1/data/transferprocess"

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
                .statusCode(200)
                .extract()
                .jsonPath();

        String transferProcessId = jsonPath.get("id");

        Assertions.assertThat(transferProcessId).isNotEmpty();
    }

    DataRequest readAndUpdateTransferJsonFile(File transferJsonFile, String contractAgreementId) throws IOException {
        // create object mapper instance
        ObjectMapper mapper = new ObjectMapper();

        // convert JSON file to map
        DataRequest sampleDataRequest = mapper.readValue(transferJsonFile, DataRequest.class);

        DataAddress newDataDestination = sampleDataRequest.getDataDestination().toBuilder().property("path", destinationFile.getAbsolutePath()).build();
        DataRequest newSampleDataRequest = sampleDataRequest.toBuilder().contractId(contractAgreementId).dataDestination(newDataDestination).build();

        return newSampleDataRequest;
    }

    void assertConfigPropertiesUniquePorts() {
        // test sample guidance: Create the connectors / Consumer connector
        // read both config files
        // samples/04.0-file-transfer/consumer/config.properties
        // samples/04.0-file-transfer/provider/config.properties
        // ensure each port is defined only once

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

    private void assertPortDefinedOnce(Properties properties, String portPropertyName, HashMap<Integer, String> configuredPorts, String participantName) {

        String propertyValue = properties.getProperty(portPropertyName);

        if (propertyValue == null) return;

        int portNumber = Integer.parseInt(propertyValue);

        if (configuredPorts.containsKey(portNumber)) {
            Assertions.fail(String.format("Port number '%d' defined multiple times.", portNumber));
        }

        configuredPorts.put(portNumber, String.format("%s/%s", participantName, portPropertyName));
    }
}
