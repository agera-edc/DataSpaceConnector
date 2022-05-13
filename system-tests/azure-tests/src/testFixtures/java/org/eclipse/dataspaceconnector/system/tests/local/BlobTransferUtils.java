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

import org.jetbrains.annotations.NotNull;

import static io.restassured.RestAssured.given;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_PROCESSES_PATH;

public class BlobTransferUtils {

    @NotNull
    public static String getProvisionedContainerName() {
        return given()
                .baseUri(CONSUMER_CONNECTOR_MANAGEMENT_URL + CONSUMER_MANAGEMENT_PATH)
                .when()
                .get(TRANSFER_PROCESSES_PATH)
                .then()
                .statusCode(200)
                .extract().body()
                .jsonPath().getString("[0].dataDestination.properties.container");
    }
}
