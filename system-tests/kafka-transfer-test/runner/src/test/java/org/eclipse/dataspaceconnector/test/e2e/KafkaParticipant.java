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
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;

class KafkaParticipant {

    private static final String IDS_PATH = "/api/v1/ids";

    public static final String KAFKA_SERVER = "localhost:29092";
    public static final String KAFKA_TOPIC = "test_events";

    private final Duration timeout = Duration.ofSeconds(30);

    private final URI controlPlane;
    private final URI idsEndpoint;

    KafkaParticipant(String controlPlane, String idsEndpoint) {
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
                                "type", "Kafka",
                                "topic", KAFKA_TOPIC,
                                "kafka.bootstrap.servers", KAFKA_SERVER,
                                "kafka.key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                                "kafka.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                                "kafka.max.poll.records", "100"
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

    public String negotiateContract(String assetId, KafkaParticipant provider) {
        var policy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .permission(Permission.Builder.newInstance()
                        .target(assetId)
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();
        var request = Map.of(
                "connectorId", "provider",
                "connectorAddress", provider.idsEndpoint() + "/api/v1/ids/data",
                "protocol", "ids-multipart",
                "offer", Map.of(
                        "offerId", "1:1",
                        "assetId", assetId,
                        "policy", policy
                )
        );

        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(request)
                .when()
                .post("/contractnegotiations")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("id");
    }

    public String getContractAgreementId(String negotiationId) {
        var contractAgreementId = new AtomicReference<String>();

        await().atMost(timeout).untilAsserted(() -> {
            var result = given()
                    .baseUri(controlPlane.toString())
                    .contentType(JSON)
                    .when()
                    .get("/contractnegotiations/{id}", negotiationId)
                    .then()
                    .statusCode(200)
                    .body("contractAgreementId", notNullValue())
                    .extract().body().jsonPath().getString("contractAgreementId");

            contractAgreementId.set(result);
        });

        return contractAgreementId.get();
    }

    public String dataRequest(String contractAgreementId, String assetId, KafkaParticipant provider, DataAddress dataAddress) {
        var request = Map.of(
                "contractId", contractAgreementId,
                "assetId", assetId,
                "connectorId", "provider",
                "connectorAddress", provider.idsEndpoint() + "/api/v1/ids/data",
                "protocol", "ids-multipart",
                "dataDestination", dataAddress,
                "managedResources", false,
                "transferType", TransferType.Builder.transferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                        .build()
        );

        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(request)
                .when()
                .post("/transferprocess")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("id");
    }

    public String getTransferProcessState(String transferProcessId) {
        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .when()
                .get("/transferprocess/{id}/state", transferProcessId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("state");
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
