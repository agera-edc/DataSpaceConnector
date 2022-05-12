package org.eclipse.dataspaceconnector.extension.sample.test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.*;

import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.assertj.core.api.Assertions.assertThat;
import io.restassured.RestAssured.*;
import io.restassured.matcher.RestAssuredMatchers.*;
import org.hamcrest.Matchers.*;

public class FileTransferSampleTest {

    public static final int CONSUMER_CONNECTOR_PORT = 9191;
    public static final int CONSUMER_MANAGEMENT_PORT = 9192;
    public static final String CONSUMER_CONNECTOR_PATH = "/api";
    public static final String CONSUMER_MANAGEMENT_PATH = "/api/v1/data";
    public static final String CONSUMER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + CONSUMER_MANAGEMENT_PORT;
    public static final int CONSUMER_IDS_API_PORT = 9292;
    public static final String CONSUMER_IDS_API = "http://localhost:" + CONSUMER_IDS_API_PORT;

    public static final int PROVIDER_CONNECTOR_PORT = 8181;
    public static final int PROVIDER_MANAGEMENT_PORT = 8182;
    public static final String PROVIDER_CONNECTOR_PATH = "/api";
    public static final String PROVIDER_MANAGEMENT_PATH = "/api/v1/data";
    public static final String PROVIDER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + PROVIDER_MANAGEMENT_PORT;
    public static final int PROVIDER_IDS_API_PORT = 8282;
    public static final String PROVIDER_IDS_API = "http://localhost:" + PROVIDER_IDS_API_PORT;
    public static final String IDS_PATH = "/api/v1/ids";

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_KEY = "password";

    // Starting EDC runtimes implicitly aligns to Run the sample / 1. Build and start the connectors.
    // TODO: Read properties from config.properties files for consumer and provider.
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
                    "edc.samples.04.asset.path", "samples/04.0-file-transfer/provider/test.txt",
                    "edc.ids.id", "urn:connector:provider"
            )
    );

    @Test
    void testSample() {
        assertThat(1 + 2).isEqualTo(3);
    }

    @Test
    void initiateContractNegotiation() {
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


    }

    @Test
    void configPropertiesUniquePorts() throws IOException {
        // test sample guidance: Create the connectors / Consumer connector
        // read both config files
        // samples/04.0-file-transfer/consumer/config.properties
        // samples/04.0-file-transfer/provider/config.properties
        // ensure each port is defined only once

        String pathConsumerConfigProperties = new File(TestUtils.findBuildRoot(), "samples/04.0-file-transfer/consumer/config.properties").getAbsolutePath();
        String pathProviderConfigProperties = new File(TestUtils.findBuildRoot(), "samples/04.0-file-transfer/provider/config.properties").getAbsolutePath();

        Properties consumerProperties = readPropertiesFile(pathConsumerConfigProperties);
        Properties providerProperties = readPropertiesFile(pathProviderConfigProperties);

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
