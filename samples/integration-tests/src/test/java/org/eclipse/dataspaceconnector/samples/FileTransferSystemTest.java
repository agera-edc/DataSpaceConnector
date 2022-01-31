/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.samples;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

/**
 * System Test for Sample 04.0-file-transfer
 */
@Tag("SystemTests")
@IntegrationTest
public class FileTransferSystemTest {

    private static final String CONTRACT_NEGOTIATION_PATH = "/api/negotiation";
    private static final String CONTRACT_AGREEMENT_PATH = "/api/control/negotiation/{contractNegotiationRequestId}";
    private static final String FILE_TRANSFER_PATH = "/api/file/test-document";

    private static final String CONNECTOR_ADDRESS_PARAM = "connectorAddress";
    private static final String CONTRACT_NEGOTIATION_REQUEST_ID_PARAM = "contractNegotiationRequestId";
    private static final String DESTINATION_PARAM = "destination";
    private static final String CONTRACT_ID_PARAM = "contractId";

    private static final String CONSUMER_CONNECTOR_HOST = propOrEnv("edc.consumer.connector.host", "http://localhost:9191");
    private static final String CONSUMER_ASSET_PATH = propOrEnv("edc.samples.04.consumer.asset.path", "/tmp/consumer");

    private static final String PROVIDER_CONNECTOR_HOST = propOrEnv("edc.provider.connector.host", "http://localhost:8181");
    private static final String PROVIDER_ASSET_PATH = propOrEnv("edc.samples.04.asset.path", "/tmp/provider/test-document.txt");

    private static final String API_KEY_CONTROL_AUTH = propOrEnv("edc.api.control.auth.apikey.value", "password");
    private static final String API_KEY_HEADER = "X-Api-Key";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = CONSUMER_CONNECTOR_HOST;
    }

    @Test
    public void transferFile_success() {
        //Arrange
        var contractOffer = TestUtils.getFileFromResourceName("contractoffer.json");

        //Act & Assert

        // Initiate a contract negotiation
        var contractNegotiationRequestId =
                given()
                        .contentType(ContentType.JSON)
                        .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
                        .body(contractOffer)
                        .when()
                        .post(CONTRACT_NEGOTIATION_PATH)
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().asString();

        // UUID is returned to get the contract agreement negotiated between provider and consumer.
        assertThat(contractNegotiationRequestId).isNotBlank();

        // Verify ContractNegotiation is CONFIRMED (state = 1200)
        await().atMost(30, SECONDS).untilAsserted(() -> {

            assertThatJson(getNegotiatedAgreement(contractNegotiationRequestId).toString()).and(
                    json -> json.node("id").isEqualTo(contractNegotiationRequestId),
                    json -> json.node("state").isEqualTo(1200),
                    json -> json.node("contractAgreement.id").isNotNull()
            );
        });

        // Obtain contract agreement ID
        var contractAgreementId = getNegotiatedAgreement(contractNegotiationRequestId)
                .get("contractAgreement").get("id").textValue();

        //Initiate file transfer
        var transferProcessId =
                given()
                        .noContentType()
                        .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
                        .queryParam(DESTINATION_PARAM, CONSUMER_ASSET_PATH)
                        .queryParam(CONTRACT_ID_PARAM, contractAgreementId)
                        .when()
                        .post(FILE_TRANSFER_PATH)
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().asString();

        assertThat(transferProcessId).isNotNull();
    }

    private ObjectNode getNegotiatedAgreement(String contractNegotiationRequestId) {
        return
                given()
                        .pathParam(CONTRACT_NEGOTIATION_REQUEST_ID_PARAM, contractNegotiationRequestId)
                        .header(API_KEY_HEADER, API_KEY_CONTROL_AUTH)
                        .when()
                        .get(CONTRACT_AGREEMENT_PATH)
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().as(ObjectNode.class);
    }
}
