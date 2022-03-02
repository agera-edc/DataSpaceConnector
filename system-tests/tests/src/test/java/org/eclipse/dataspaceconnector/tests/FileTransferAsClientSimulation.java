package org.eclipse.dataspaceconnector.tests;

import com.github.javafaker.Faker;
import io.gatling.javaapi.core.Simulation;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.dataspaceconnector.tests.FileTransferSimulationUtils.DESCRIPTION;
import static org.eclipse.dataspaceconnector.tests.FileTransferSimulationUtils.contractNegotiationAndFileTransfer;

/**
 * Runs a single iteration of {@see FileTransferSimulation}, getting settings from environment variables.
 */
public class FileTransferAsClientSimulation extends Simulation {

    private Faker faker = new Faker();

    public FileTransferAsClientSimulation() {
        setUp(scenario(DESCRIPTION)
                .repeat(1)
                .on(
                        contractNegotiationAndFileTransfer(
                                getFromEnv("PROVIDER_URL"),
                                getFromEnv("DESTINATION_PATH"),
                                getFromEnv("API_KEY"))
                )
                .injectOpen(atOnceUsers(1)))
                .protocols(http
                        .baseUrl(getFromEnv("CONSUMER_URL")))
                .assertions(
                        global().responseTime().max().lt(2000),
                        global().successfulRequests().percent().is(100.0)
                );
    }

    private static String getFromEnv(String env) {
        return Objects.requireNonNull(StringUtils.trimToNull(System.getenv(env)), env);
    }
}
