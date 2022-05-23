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

package org.eclipse.dataspaceconnector.test.e2e;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;

import java.net.URI;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

class IdsDidAuthParticipant {

    private static final String IDS_PATH = "/api/v1/ids";

    private final URI controlPlane;
    private final URI idsEndpoint;

    IdsDidAuthParticipant(String controlPlane, String idsEndpoint) {
        this.controlPlane = URI.create(controlPlane);
        this.idsEndpoint = URI.create(idsEndpoint);
    }

    public void createAsset(String assetId) {
        var asset = Map.of(
                "asset", Map.of(
                        "properties", Map.of(
                                "asset:prop:id", assetId,
                                "asset:prop:name", "asset name",
                                "asset:prop:contenttype", "text/plain",
                                "asset:prop:policy-id", "use-eu"
                        )
                ),
                "dataAddress", Map.of(
                        "properties", Map.of(
                                "name", "data",
                                "type", "HttpData"
                        )
                )
        );

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(asset)
                .when()
                .post("/assets")
                .then()
                .statusCode(204);
    }

    public String createPolicy(String assetId) {
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target(assetId)
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(policy)
                .when()
                .post("/policies")
                .then()
                .statusCode(204);

        return policy.getUid();
    }

    public void createContractDefinition(String policyId) {
        var contractDefinition = Map.of(
                "id", "1",
                "accessPolicyId", policyId,
                "contractPolicyId", policyId,
                "criteria", AssetSelectorExpression.SELECT_ALL.getCriteria()
        );

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(contractDefinition)
                .when()
                .post("/contractdefinitions")
                .then()
                .statusCode(204);
    }

    public Catalog getCatalog(URI provider) {
        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .when()
                .queryParam("providerUrl", provider + IDS_PATH + "/data")
                .get("/catalog")
                .then()
                .statusCode(200)
                .extract().body().as(Catalog.class);
    }

    public URI idsEndpoint() {
        return idsEndpoint;
    }
}
