package org.eclipse.dataspaceconnector.tests;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.API_KEY_CONTROL_AUTH;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.CONSUMER_ASSET_PATH;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.CONSUMER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.PROVIDER_CONNECTOR_HOST;

public class FileTransferLocalSimulation extends FileTransferSimulation {

    public FileTransferLocalSimulation() {
        super(
                PROVIDER_CONNECTOR_HOST,
                CONSUMER_ASSET_PATH,
                API_KEY_CONTROL_AUTH,
                1);

        setUp(scenarioBuilder
                .injectOpen(atOnceUsers(1)))
                .protocols(http
                        .baseUrl(CONSUMER_CONNECTOR_HOST))
                .assertions(
                        global().responseTime().max().lt(2000),
                        global().successfulRequests().percent().is(100.0)
                );
    }
}
