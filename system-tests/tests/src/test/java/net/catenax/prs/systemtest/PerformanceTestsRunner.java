package net.catenax.prs.systemtest;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

import java.time.Duration;
import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.*;
import static org.eclipse.dataspaceconnector.tests.FileTransferTestUtils.*;

public class PerformanceTestsRunner extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(CONSUMER_CONNECTOR_HOST)
            .acceptHeader("*/*")
            .contentTypeHeader("application/json");

    protected ScenarioBuilder scenarioBuilder = scenario("Contract negotiation and data transfer.")
            .repeat(1)
            .on(exec(
                            http("Contract negotiation")
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
                                    doWhileDuring(session -> session.getInt("status") !=
                                                    ContractNegotiationStates.CONFIRMED.code()
                                            , Duration.ofSeconds(30))
                                            .on(exec(http("Get status")
                                                            .get(session -> format("/api/control/negotiation/%s", session.getString("contractNegotiationRequestId")))
                                                            .header(API_KEY_HEADER, API_KEY_CONTROL_AUTH)
                                                            .check(status().is(HttpStatus.SC_OK))
                                                            .check(
                                                                    jmesPath("id").is(session -> session.getString("contractNegotiationRequestId")),
                                                                    jmesPath("state").saveAs("status")
                                                            )
                                                            .checkIf(
                                                                    session -> Integer.toString(ContractNegotiationStates.CONFIRMED.code()).equals(session.getString("status"))
                                                            )
                                                            .then(
                                                                    jmesPath("contractAgreement.id").notNull().saveAs("contractAgreementId")
                                                            )
                                                    )
                                                            .pause(Duration.ofSeconds(1))
                                            )

                            )
            );

    {
        setUp(scenarioBuilder.injectOpen(atOnceUsers(10))).protocols(httpProtocol);
    }
}
