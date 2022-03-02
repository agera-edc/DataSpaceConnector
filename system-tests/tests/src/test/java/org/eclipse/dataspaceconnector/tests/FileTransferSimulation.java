package org.eclipse.dataspaceconnector.tests;

import com.github.javafaker.Faker;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.tests.GatlingUtils.endlesslyWith;
import static org.apache.http.HttpStatus.SC_OK;

public abstract class FileTransferSimulation extends Simulation {

    public static final String PROVIDER_ASSET_NAME = "test-document";
    private static final String CONNECTOR_ADDRESS_PARAM = "connectorAddress";
    private static final String DESTINATION_PARAM = "destination";
    private static final String CONTRACT_ID_PARAM = "contractId";
    private static final String API_KEY_HEADER = "X-Api-Key";
    protected final ScenarioBuilder scenarioBuilder;


    private Faker faker = new Faker();

    protected FileTransferSimulation(String providerUrl, String destinationPath, String apiKey, int times) {
        String connectorAddress = format("%s/api/ids/multipart", providerUrl);
        scenarioBuilder = scenario("Contract negotiation and data transfer.")
                .repeat(times)
                .on(exec(
                                http("Contract negotiation")
                                        .post("/api/negotiation")
                                        .body(InputStreamBody(s -> Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("contractoffer.json"))))
                                        .header(CONTENT_TYPE, "application/json")
                                        .queryParam(CONNECTOR_ADDRESS_PARAM, connectorAddress)
                                        .check(status().is(SC_OK))
                                        .check(bodyString()
                                                .notNull()
                                                .saveAs("contractNegotiationRequestId"))
                        )
                                // Call status endpoint every second, till it gives a 200 status code.
                                // Verify ContractNegotiation is CONFIRMED
                                .exec(session -> session.set("status", -1))
                                .group("waitForCompletion").on(
                                        doWhileDuring(session -> session.getString("contractAgreementId") == null,
                                                Duration.ofSeconds(30))
                                                .on(exec(http("Get status")
                                                                .get(session -> format("/api/control/negotiation/%s", session.getString("contractNegotiationRequestId")))
                                                                .header(API_KEY_HEADER, apiKey)
                                                                .check(status().is(SC_OK))
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
                                                                .pace(Duration.ofSeconds(1))
                                                )

                                )
                                .feed(endlesslyWith(() -> Map.of("fileName", faker.lorem().characters(20, 40))))
                                .exec(
                                        http("Initiate transfer")
                                                .post(format("/api/file/%s", PROVIDER_ASSET_NAME))
                                                .queryParam(CONNECTOR_ADDRESS_PARAM, connectorAddress)
                                                .queryParam(DESTINATION_PARAM, s -> format(destinationPath, s.getString("fileName"))) // TODO %s
                                                .queryParam(CONTRACT_ID_PARAM, s -> s.getString("contractAgreementId"))
                                                .check(status().is(SC_OK))
                                                .check(bodyString()
                                                        .notNull()
                                                        .saveAs("transferProcessId"))
                                )
                                .exec(session -> session.set("status", -1))
                                // Verify file transfer is completed and file contents
                                .group("waitForTransferCompletion").on(
                                        doWhileDuring(session -> session.getInt("status") != TransferProcessStates.COMPLETED.code(),
                                                Duration.ofSeconds(30))
                                                .on(exec(http("Get transfer status")
                                                                .get(session -> format("/api/transfer/%s", session.getString("transferProcessId")))
                                                                .check(status().is(SC_OK))
                                                                .check(
                                                                        jmesPath("id").is(session -> session.getString("transferProcessId")),
                                                                        jmesPath("state").saveAs("status")
                                                                )
                                                        )
                                                                .pace(Duration.ofSeconds(1))
                                                )

                                )
                );

    }

}
