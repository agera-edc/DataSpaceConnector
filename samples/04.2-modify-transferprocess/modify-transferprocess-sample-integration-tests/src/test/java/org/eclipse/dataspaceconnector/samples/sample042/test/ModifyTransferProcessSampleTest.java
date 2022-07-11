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
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

package org.eclipse.dataspaceconnector.samples.sample042.test;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.extension.sample.test.FileTransferSampleTestCommon.API_KEY_HEADER_KEY;
import static org.eclipse.dataspaceconnector.extension.sample.test.FileTransferSampleTestCommon.API_KEY_HEADER_VALUE;
import static org.eclipse.dataspaceconnector.extension.sample.test.FileTransferSampleTestCommon.getFileFromRelativePath;
import static org.hamcrest.Matchers.equalTo;

@EndToEndTest
public class ModifyTransferProcessSampleTest {

    static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.2-modify-transferprocess/consumer/config.properties";
    static final String TRANSFER_PROCESS_URI = "http://localhost:9192/api/v1/data/transferprocess";
    public static final String EXPECTED_ID_PROPERTY_NAME = "id[0]";
    public static final String EXPECTED_ID_PROPERTY_VALUE = "tp-sample-04.2";
    public static final String EXPECTED_STATE_PROPERTY_NAME = "state[0]";
    public static final String EXPECTED_STATE_PROPERTY_VALUE = "ERROR";
    public static final String EXPECTED_ERROR_DETAIL_PROPERTY_NAME = "errorDetail[0]";
    public static final String EXPECTED_ERROR_DETAIL_PROPERTY_VALUE = "timeout by watchdog";
    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.2-modify-transferprocess:consumer",
            "consumer",
            Map.of(
                    "edc.fs.config", getFileFromRelativePath(CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    static final Duration DURATION = Duration.ofSeconds(15);
    static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    /**
     * Requests transfer processes from data management API and check for expected changes on the transfer process.
     */
    @Test
    void runSample() {
        await().atMost(DURATION).pollInterval(POLL_INTERVAL)
                .untilAsserted(() ->
                        RestAssured
                                .given()
                                    .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                                .when()
                                    .get(TRANSFER_PROCESS_URI)
                                .then()
                                    .statusCode(HttpStatus.SC_OK)
                                    .body(
                                            EXPECTED_ID_PROPERTY_NAME, equalTo(EXPECTED_ID_PROPERTY_VALUE),
                                            EXPECTED_STATE_PROPERTY_NAME, equalTo(EXPECTED_STATE_PROPERTY_VALUE),
                                            EXPECTED_ERROR_DETAIL_PROPERTY_NAME, equalTo(EXPECTED_ERROR_DETAIL_PROPERTY_VALUE)
                                    )
        );
    }
}
