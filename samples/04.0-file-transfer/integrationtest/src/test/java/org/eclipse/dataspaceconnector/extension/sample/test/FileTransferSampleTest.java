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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.equalTo;

public class FileTransferSampleTest {

    private String contractNegotiationId;
    private String contractAgreementId;
    private String transferProcessId;

    public static final int CONSUMER_CONNECTOR_PORT = 9191;
    public static final int CONSUMER_MANAGEMENT_PORT = 9192;
    public static final String CONSUMER_CONNECTOR_PATH = "/api";
    public static final String CONSUMER_MANAGEMENT_PATH = "/api/v1/data";
    //public static final String CONSUMER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + CONSUMER_MANAGEMENT_PORT;
    public static final int CONSUMER_IDS_API_PORT = 9292;
    public static final String CONSUMER_IDS_API = "http://localhost:" + CONSUMER_IDS_API_PORT;
    public static final int PROVIDER_CONNECTOR_PORT = 8181;
    public static final int PROVIDER_MANAGEMENT_PORT = 8182;
    public static final String PROVIDER_CONNECTOR_PATH = "/api";
    public static final String PROVIDER_MANAGEMENT_PATH = "/api/v1/data";
    //public static final String PROVIDER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + PROVIDER_MANAGEMENT_PORT;
    public static final int PROVIDER_IDS_API_PORT = 8282;
    public static final String PROVIDER_IDS_API = "http://localhost:" + PROVIDER_IDS_API_PORT;
    public static final String IDS_PATH = "/api/v1/ids";

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_KEY = "password";
    private static final String DESTINATION_PATH = "samples/04.0-file-transfer/consumer/requested.test.txt";
    // Reuse an already existing file for the test. Could be set to any other existing file in the repository.
    private static final String SAMPLE_ASSET_PATH = "samples/04.0-file-transfer/filetransfer.json";


    // Starting EDC runtimes implicitly aligns to Run the sample / 1. Build and start the connectors.
    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:consumer",
            "consumer",
            Map.of(
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "web.http.path", CONSUMER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
                    "web.http.data.path", CONSUMER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "ids.webhook.address", CONSUMER_IDS_API,
                    "edc.api.auth.key", API_KEY,
                    "edc.ids.id", "urn:connector:consumer"
                    // Keep the following line commented out until both EdcRuntimeExtension can be used which is
                    // currently not possible because CONFIG_LOCATION is static in
                    // org/eclipse/dataspaceconnector/configuration/fs/FsConfigurationExtension.java
//                    "edc.fs.config", new File(TestUtils.findBuildRoot(),"samples/04.0-file-transfer/consumer/config.properties").getAbsolutePath()
            )
    );

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:provider",
            "provider",
            Map.of(
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "web.http.path", PROVIDER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(PROVIDER_MANAGEMENT_PORT),
                    "web.http.data.path", PROVIDER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "ids.webhook.address", PROVIDER_IDS_API,
                    "edc.ids.id", "urn:connector:provider",
                    // Even if property 'edc.fs.config' is used, keep the following line to override
                    // 'edc.samples.04.asset.path' from 'samples/04.0-file-transfer/provider/config.properties'.
                    "edc.samples.04.asset.path", new File(TestUtils.findBuildRoot(), SAMPLE_ASSET_PATH).getAbsolutePath()
                    // Keep the following line commented out until both EdcRuntimeExtension can be used which is
                    // currently not possible because CONFIG_LOCATION is static in
                    // org/eclipse/dataspaceconnector/configuration/fs/FsConfigurationExtension.java
//                    "edc.fs.config", new File(TestUtils.findBuildRoot(),"samples/04.0-file-transfer/provider/config.properties").getAbsolutePath()
            )
    );

    /**
     * Run all sample steps in one single test.
     * Note: Sample steps cannot be separated into single tests because {@link EdcRuntimeExtension} runs before each single test.
     * */
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
        var transferredFile = new File(TestUtils.findBuildRoot(), DESTINATION_PATH);

        Assertions.assertThat(transferredFile.exists()).isFalse();
    }

    private void cleanTemporaryTestFiles() {
        var transferredFile = new File(TestUtils.findBuildRoot(), DESTINATION_PATH);

        if (transferredFile.exists()) {
            transferredFile.delete();
        }
    }

    void assertWaitForDestinationFileExistence() {
        var expectedFile = new File(TestUtils.findBuildRoot(), DESTINATION_PATH);

        await().atMost(TIMEOUT).pollInterval(1000, MILLISECONDS).untilAsserted(() ->
            Assertions.assertThat(expectedFile.exists()).isTrue()
        );
    }

    void assertInitiateContractNegotiation() {
        // curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/contractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"

        JsonPath jsonPath = RestAssured
                .given()
                    .headers(API_KEY_HEADER, API_KEY)
                    .contentType(ContentType.JSON)
                    .body(new File(TestUtils.findBuildRoot(), "samples/04.0-file-transfer/contractoffer.json"))
                    .log().all()
                .when()
                    .post("http://localhost:9192/api/v1/data/contractnegotiations")
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
        await().atMost(TIMEOUT).pollInterval(1000, MILLISECONDS).untilAsserted(() ->  {
                var result = RestAssured
                    .given()
                        .headers(API_KEY_HEADER, API_KEY)
                        .log().all()
                    .when()
                        .get("http://localhost:9192/api/v1/data/contractnegotiations/{id}", contractNegotiationId)
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

        File transferJsonFile = new File(TestUtils.findBuildRoot(), "samples/04.0-file-transfer/filetransfer.json");
        DataRequest sampleDataRequest = readAndUpdateTransferJsonFile(transferJsonFile, contractAgreementId, DESTINATION_PATH);

        JsonPath jsonPath = RestAssured
            .given()
                .headers(API_KEY_HEADER, API_KEY)
                .contentType(ContentType.JSON)
                .body(sampleDataRequest)
                .log().all()
            .when()
                .post("http://localhost:9192/api/v1/data/transferprocess")
            .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        transferProcessId = jsonPath.get("id");
    }

    DataRequest readAndUpdateTransferJsonFile(File transferJsonFile, String contractAgreementId, String destinationPath) throws IOException {
        // create object mapper instance
        ObjectMapper mapper = new ObjectMapper();

        // convert JSON file to map
        DataRequest sampleDataRequest = mapper.readValue(transferJsonFile, DataRequest.class);

        String absolutePathDestination = new File(TestUtils.findBuildRoot(), destinationPath).getAbsolutePath();

        DataAddress newDataDestination = sampleDataRequest.getDataDestination().toBuilder().property("path", absolutePathDestination).build();
        DataRequest newSampleDataRequest = sampleDataRequest.toBuilder().contractId(contractAgreementId).dataDestination(newDataDestination).build();

        return newSampleDataRequest;
    }


    void assertConfigPropertiesUniquePorts() {
        // test sample guidance: Create the connectors / Consumer connector
        // read both config files
        // samples/04.0-file-transfer/consumer/config.properties
        // samples/04.0-file-transfer/provider/config.properties
        // ensure each port is defined only once

        String pathConsumerConfigProperties = new File(TestUtils.findBuildRoot(), "samples/04.0-file-transfer/consumer/config.properties").getAbsolutePath();
        String pathProviderConfigProperties = new File(TestUtils.findBuildRoot(), "samples/04.0-file-transfer/provider/config.properties").getAbsolutePath();

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

    private static Properties readPropertiesFile(String fileName) throws IOException {
        FileInputStream fis = null;
        Properties prop = null;

        try {
            fis = new FileInputStream(fileName);
            prop = new Properties();
            prop.load(fis);

        } catch (IOException fnfe) {
            fnfe.printStackTrace();
        } finally {
            if (fis != null) fis.close();
        }

        return prop;
    }
}
