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

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

/**
 * System Test for Sample 04.0-file-transfer
 */
@Tag("SystemTests")
@IntegrationTest
public class FileTransferSystemTest {

    private static final String CONTRACT_NEGOTIATION_PATH = "/api/negotiation";
    private static final String CONTRACT_AGREEMENT_PATH = "/api/control/negotiation/{contractNegotiationRequestId}";
    private static final String CONNECTOR_ADDRESS = "connectorAddress";
    private static final String CONTRACT_NEGOTIATION_REQUEST_ID = "contractNegotiationRequestId";
    private static final String CONSUMER_CONNECTOR_HOST = propOrEnv("edc.consumer.connector.host", "http://localhost:9191");
    private static final String PROVIDER_CONNECTOR_HOST = propOrEnv("edc.provider.connector.host", "http://localhost:8181");
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

        //Act

        // Initiate a contract negotiation
        var contractNegotiationRequestId =
                given()
                        .contentType(ContentType.JSON)
                        .queryParam(CONNECTOR_ADDRESS, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
                        .body(contractOffer)
                        .when()
                        .post(CONTRACT_NEGOTIATION_PATH)
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().asString();

        // UUID is returned to get the contract agreement negotiated between provider and consumer.
        assertThat(contractNegotiationRequestId).isNotBlank();

        // Verify ContractNegotiation is CONFIRMED (state = 1200)
        await().atMost(60, SECONDS).untilAsserted(() -> {

            assertThatJson(getNegotiatedAgreement(contractNegotiationRequestId)).and(
                    json -> json.node("id").isEqualTo(contractNegotiationRequestId),
                    json -> json.node("state").isEqualTo(1200),
                    json -> json.node("contractAgreement.id").isNotNull()
            );
        });

//        //Get contract agreement id.
//        var contractAgreementId = assertThatJson(getNegotiatedAgreement(contractNegotiationRequestId))
//                .node("contractAgreement.id").asString();


    }

    private String getNegotiatedAgreement(String contractNegotiationRequestId) {
        return
                given()
                        .pathParam(CONTRACT_NEGOTIATION_REQUEST_ID, contractNegotiationRequestId)
                        .header(API_KEY_HEADER, API_KEY_CONTROL_AUTH)
                        .when()
                        .get(CONTRACT_AGREEMENT_PATH)
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().asString();
    }
}
