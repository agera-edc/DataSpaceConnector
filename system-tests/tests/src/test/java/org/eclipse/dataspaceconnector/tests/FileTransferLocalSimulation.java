package org.eclipse.dataspaceconnector.tests;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.API_KEY_CONTROL_AUTH;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.CONSUMER_ASSET_PATH;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.CONSUMER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.PROVIDER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.tests.FileTransferSimulationUtils.DESCRIPTION;
import static org.eclipse.dataspaceconnector.tests.FileTransferSimulationUtils.contractNegotiationAndFileTransfer;

/**
 * Runs a single iteration of {@see FileTransferSimulation}, getting settings from
 * {@see FileTransferIntegrationTest}.
 */
public class FileTransferLocalSimulation extends Simulation {

    public FileTransferLocalSimulation() {

        setUp(scenario(DESCRIPTION)
                .repeat(1)
                .on(
                        contractNegotiationAndFileTransfer(PROVIDER_CONNECTOR_HOST, CONSUMER_ASSET_PATH, API_KEY_CONTROL_AUTH)
                )
                .injectOpen(atOnceUsers(1)))
                .protocols(http
                        .baseUrl(CONSUMER_CONNECTOR_HOST))
                .assertions(
                        global().responseTime().max().lt(2000),
                        global().successfulRequests().percent().is(100.0)
                );
    }
}
