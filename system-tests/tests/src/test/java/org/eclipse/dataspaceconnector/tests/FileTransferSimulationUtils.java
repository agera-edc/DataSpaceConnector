package org.eclipse.dataspaceconnector.tests;

import com.github.javafaker.Faker;
import io.gatling.javaapi.core.ChainBuilder;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.doWhileDuring;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.eclipse.dataspaceconnector.tests.GatlingUtils.endlesslyWith;

/**
 * Utility methods for building a Gatling simulation for performing contract negotiation and file transfer.
 */
public abstract class FileTransferSimulationUtils {

    private FileTransferSimulationUtils() {
    }

    public static final String DESCRIPTION = "[Contract negotiation and file transfer]";

    public static final String PROVIDER_ASSET_NAME = "test-document";
    private static final String CONNECTOR_ADDRESS_PARAM = "connectorAddress";
    private static final String DESTINATION_PARAM = "destination";
    private static final String CONTRACT_ID_PARAM = "contractId";
    private static final String API_KEY_HEADER = "X-Api-Key";

    private static final Faker faker = new Faker();

    /**
     * Gatling chain for performing contract negotiation and file transfer.
     *
     * @param providerUrl     URL for the Provider API, as accessed from the Consumer runtime.
     * @param destinationPath File copy destination path. If it includes the character sequence {@code %s}, that sequence is replaced with a random string in each iteration.
     * @param apiKey          Consumer runtime API Key.
     */
    protected static ChainBuilder contractNegotiationAndFileTransfer(String providerUrl, String destinationPath, String apiKey) {
        String connectorAddress = format("%s/api/ids/multipart", providerUrl);
        String body;
        try {
            body = new String(Thread.currentThread().getContextClassLoader().getResourceAsStream("contractoffer.json").readAllBytes());
        } catch (IOException e) {
            throw new EdcException(e);
        }
        return group("Contract negotiation")
                .on(exec(
                        // Initiate a contract negotiation
                        http("Contract negotiation")
                                .post("/api/negotiation")
                                .body(StringBody(body))
                                .header(CONTENT_TYPE, "application/json")
                                .queryParam(CONNECTOR_ADDRESS_PARAM, connectorAddress)
                                .check(status().is(SC_OK))
                                .check(bodyString()
                                        .notNull()
                                        // UUID is returned to get the contract agreement negotiated between provider and consumer.

                                        .saveAs("contractNegotiationRequestId"))
                ))
                // Call ContractNegotiation status endpoint every second, till it gives a CONFIRMED state.
                .exec(session -> session.set("status", -1))
                .group("Wait for agreement").on(
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
                                                        // Obtain contract agreement ID
                                                        jmesPath("contractAgreement.id").notNull().saveAs("contractAgreementId")
                                                )
                                        )
                                                .pace(Duration.ofSeconds(1))
                                )

                )
                .feed(endlesslyWith(() -> Map.of("fileName", faker.lorem().characters(20, 40))))
                .group("Initiate transfer")
                .on(exec(
                        // Initiate a file transfer
                        http("Initiate transfer")
                                .post(format("/api/file/%s", PROVIDER_ASSET_NAME))
                                .queryParam(CONNECTOR_ADDRESS_PARAM, connectorAddress)
                                .queryParam(DESTINATION_PARAM, s -> format(destinationPath, s.getString("fileName")))
                                .queryParam(CONTRACT_ID_PARAM, s -> s.getString("contractAgreementId"))
                                .check(status().is(SC_OK))
                                .check(bodyString()
                                        .notNull()
                                        .saveAs("transferProcessId"))
                ))
                .exec(session -> session.set("status", -1))
                // Call transfer status endpoint every second, till it gives a COMPLETED state.
                .group("Wait for transfer").on(
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
                );
    }
}
