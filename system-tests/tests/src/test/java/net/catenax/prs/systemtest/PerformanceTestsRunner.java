package net.catenax.prs.systemtest;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

import java.time.Duration;
import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.core.CoreDsl.doWhileDuring;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.CONSUMER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.PROVIDER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.tests.FileTransferTestUtils.*;

public class PerformanceTestsRunner extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(CONSUMER_CONNECTOR_HOST)
            .acceptHeader("*/*")
            .contentTypeHeader("application/json");

    protected ScenarioBuilder scenarioBuilder = scenario("Trigger Get parts tree for a part.")
            .repeat(1)
            .on(exec(
                    http("Trigger partsTree request")
                            .post(CONTRACT_NEGOTIATION_PATH)
                            .body(InputStreamBody(
                                    s -> Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("contractoffer.json"))
                            ))
                            .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
                            .check(status().is(HttpStatus.SC_OK))
                            .check(bodyString()
                                    .notNull()
                                    .saveAs("contractNegotiationRequestId"))
            )
                    // Call status endpoint every second, till it gives a 200 status code.
                    // Verify ContractNegotiation is CONFIRMED
                    .exec(session -> session.set("status", -1))
                    .group("waitForCompletion").on(
                            doWhileDuring(session -> session.getInt("status") != 200, Duration.ofSeconds(30))
                                    .on(exec(http("Get status")
                                            .get(session -> getContractNegotiationRequestId(session))
                                            .header(API_KEY_HEADER, apiKey)
                                            .check(status().is(HttpStatus.SC_OK))
                                            .check(
                                                    jmesPath("id").is(session -> session.getString("contractNegotiationRequestId")),
                                                    jmesPath("state").is(Integer.toString(ContractNegotiationStates.CONFIRMED.code())),
                                                    jmesPath("contractAgreement.id").notNull().saveAs("contractAgreementId")
                                            )
                                    )
                                            .pause(Duration.ofSeconds(1)))));

    private String getContractNegotiationRequestId(Session session) {
        String contractNegotiationRequestId = format("/api/control/negotiation/%s", session.getString("contractNegotiationRequestId"));
        return contractNegotiationRequestId;
    }

    {
        setUp(scenarioBuilder.injectOpen(atOnceUsers(10))).protocols(httpProtocol);
    }
}
