package net.catenax.prs.systemtest;

import com.github.javafaker.Faker;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.*;
import static org.eclipse.dataspaceconnector.tests.FileTransferTestUtils.*;

public class PerformanceTestsRunner extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(CONSUMER_CONNECTOR_HOST);

    private Faker faker = new Faker();
    protected ScenarioBuilder scenarioBuilder = scenario("Contract negotiation and data transfer.")
            .repeat(1)
            .on(exec(
                            http("Contract negotiation")
                                    .post(CONTRACT_NEGOTIATION_PATH)
                                    .body(InputStreamBody(s -> Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("contractoffer.json"))))
                                    .header(CONTENT_TYPE, "application/json")
                                    .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
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
                                                            .header(API_KEY_HEADER, API_KEY_CONTROL_AUTH)
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
                            .feed(new EndlessIterator<>(() -> Map.of("fileName", faker.lorem().characters(20, 40))))
                            .exec(
                                    http("Initiate transfer")
                                            .post(format("/api/file/%s", PROVIDER_ASSET_NAME))
                                            .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
                                            .queryParam(DESTINATION_PARAM, s -> format(CONSUMER_ASSET_PATH, s.getString("fileName"))) // TODO %s
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

    {
        setUp(scenarioBuilder
                .injectOpen(atOnceUsers(10)))
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().max().lt(50),
                        global().successfulRequests().percent().gt(95.0)
                );
    }

    private static class EndlessIterator<T> implements Iterator<T> {
        private final Supplier<T> supplier;

        private EndlessIterator(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public T next() {
            return supplier.get();
        }
    }
}
