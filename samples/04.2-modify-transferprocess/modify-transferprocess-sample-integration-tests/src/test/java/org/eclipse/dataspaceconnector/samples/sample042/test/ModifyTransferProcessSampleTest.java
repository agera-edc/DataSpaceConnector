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
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.junit.extensions.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

@EndToEndTest
public class ModifyTransferProcessSampleTest {

    static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.2-modify-transferprocess/consumer/config.properties";
    static final String TRANSFER_PROCESS_URI = "http://localhost:9192/api/v1/data/transferprocess";
    static final String API_KEY_HEADER_KEY = "X-Api-Key";
    static final String API_KEY_HEADER_VALUE = "password";
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
     * Resolves a {@link File} instance from a relative path.
     */
    @NotNull
    static File getFileFromRelativePath(String relativePath) {
        return new File(TestUtils.findBuildRoot(), relativePath);
    }


    /**
     * Let run the consumer EDC.
     */
    @Test
    void runSample() {
        getTransferProcesses();
    }

    void getTransferProcesses() {
        AtomicReference<JsonPath> result = new AtomicReference<>();

        await().atMost(DURATION).pollInterval(POLL_INTERVAL)
                .untilAsserted(() ->
                        result.set(RestAssured
                                .given()
                                    .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                                .when()
                                    .get(TRANSFER_PROCESS_URI)
                                .then()
                                    .statusCode(HttpStatus.SC_OK)
                                    .body("id", not(emptyString()))
                                    .extract()
                                    .jsonPath())
        );

        System.out.print(result);
    }
}
