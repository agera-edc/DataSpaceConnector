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

package org.eclipse.dataspaceconnector.system.tests.local;

import io.gatling.javaapi.core.Simulation;

import java.io.File;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.tempDirectory;
import static org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils.DESCRIPTION;
import static org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils.PROVIDER_ASSET_NAME;
import static org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils.contractNegotiationAndFileTransfer;

/**
 * Runs a single iteration of contract negotiation and file transfer, getting settings from
 * {@see FileTransferIntegrationTest}.
 */
public class FileTransferLocalSimulation extends Simulation {
    public static final String CONSUMER_ASSET_PATH = new File(tempDirectory(), "output.txt").getAbsolutePath();
    public static final int CONSUMER_CONNECTOR_PORT = getFreePort();
    public static final int CONSUMER_MANAGEMENT_PORT = getFreePort();
    public static final String CONSUMER_CONNECTOR_PATH = "/api";
    public static final String CONSUMER_MANAGEMENT_PATH = "/api/v1/data";
    public static final String CONSUMER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + CONSUMER_MANAGEMENT_PORT;
    public static final int CONSUMER_IDS_API_PORT = getFreePort();
    public static final String CONSUMER_IDS_API = "http://localhost:" + CONSUMER_IDS_API_PORT;

    public static final String PROVIDER_ASSET_PATH = format("%s/%s.txt", tempDirectory(), PROVIDER_ASSET_NAME);
    public static final int PROVIDER_CONNECTOR_PORT = getFreePort();
    public static final int PROVIDER_MANAGEMENT_PORT = getFreePort();
    public static final String PROVIDER_CONNECTOR_PATH = "/api";
    public static final String PROVIDER_MANAGEMENT_PATH = "/api/v1/data";
    public static final int PROVIDER_IDS_API_PORT = getFreePort();
    public static final String PROVIDER_IDS_API = "http://localhost:" + PROVIDER_IDS_API_PORT;


    private static final int REPEAT = Integer.parseInt(propOrEnv("repeat", "1"));
    private static final int AT_ONCE_USERS = Integer.parseInt(propOrEnv("at.once.users", "1"));
    private static final int MAX_RESPONSE_TIME = Integer.parseInt(propOrEnv("max.response.time", "5000"));
    private static final double SUCCESS_PERCENTAGE = Double.parseDouble(propOrEnv("success.percentage", "100.0"));

    public FileTransferLocalSimulation() {

        setUp(scenario(DESCRIPTION)
                .repeat(REPEAT)
                .on(contractNegotiationAndFileTransfer(PROVIDER_IDS_API, CONSUMER_ASSET_PATH))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(http.baseUrl(CONSUMER_CONNECTOR_MANAGEMENT_URL + "/" + CONSUMER_MANAGEMENT_PATH))
                .assertions(
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }
}
