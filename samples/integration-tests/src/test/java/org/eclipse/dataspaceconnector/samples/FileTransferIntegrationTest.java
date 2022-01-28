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
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

/**
 * Integration Test for Sample 04.0-file-transfer
 */
@IntegrationTest
public class FileTransferIntegrationTest {

    private static final String CONTRACT_NEGOTIATION_PATH = "/api/negotiation";
    private static final String CONNECTOR_ADDRESS = "connectorAddress";
    private static final String CONSUMER_CONNECTOR_HOST = propOrEnv("edc.consumer.connector.host", "http://localhost:9191");
    private static final String PROVIDER_CONNECTOR_HOST = propOrEnv("edc.provider.connector.host", "http://localhost:8181");


    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = CONSUMER_CONNECTOR_HOST;
    }

    @Test
    public void negotiateContract_success() {
        var contractOffer = TestUtils.getFileFromResourceName("contractoffer.json");

        var response =
                given()
                        .contentType(ContentType.JSON)
                        .queryParam(CONNECTOR_ADDRESS, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
                        .body(contractOffer)
                .when()
                    .post(CONTRACT_NEGOTIATION_PATH)
                .then()
                        .assertThat().statusCode(HttpStatus.SC_OK);
    }
}
