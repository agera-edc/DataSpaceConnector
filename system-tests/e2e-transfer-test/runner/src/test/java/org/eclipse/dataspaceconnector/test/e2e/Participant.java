/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.test.e2e;

import org.eclipse.dataspaceconnector.client.ApiClient;
import org.eclipse.dataspaceconnector.client.ApiClientFactory;
import org.eclipse.dataspaceconnector.client.api.AssetApi;
import org.eclipse.dataspaceconnector.client.api.CatalogApi;
import org.eclipse.dataspaceconnector.client.api.ContractDefinitionApi;
import org.eclipse.dataspaceconnector.client.api.ContractNegotiationApi;
import org.eclipse.dataspaceconnector.client.api.DataplaneSelectorApi;
import org.eclipse.dataspaceconnector.client.api.PolicyApi;
import org.eclipse.dataspaceconnector.client.api.TransferProcessApi;
import org.eclipse.dataspaceconnector.client.models.Action;
import org.eclipse.dataspaceconnector.client.models.AssetDto;
import org.eclipse.dataspaceconnector.client.models.AssetEntryDto;
import org.eclipse.dataspaceconnector.client.models.Catalog;
import org.eclipse.dataspaceconnector.client.models.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.client.models.ContractOffer;
import org.eclipse.dataspaceconnector.client.models.ContractOfferDescription;
import org.eclipse.dataspaceconnector.client.models.Criterion;
import org.eclipse.dataspaceconnector.client.models.DataAddress;
import org.eclipse.dataspaceconnector.client.models.DataAddressDto;
import org.eclipse.dataspaceconnector.client.models.DataPlaneInstanceImpl;
import org.eclipse.dataspaceconnector.client.models.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.client.models.Permission;
import org.eclipse.dataspaceconnector.client.models.Policy;
import org.eclipse.dataspaceconnector.client.models.TransferRequestDto;
import org.eclipse.dataspaceconnector.client.models.TransferType;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.test.e2e.postgresql.PostgresqlLocalInstance;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;

public class Participant {

    private static final String IDS_PATH = "/api/v1/ids";
    private final Duration timeout = Duration.ofSeconds(30);

    private final URI controlPlane = URI.create("http://localhost:" + getFreePort());
    private final URI controlPlaneValidation = URI.create("http://localhost:" + getFreePort() + "/validation");
    private final URI controlPlaneDataplane = URI.create("http://localhost:" + getFreePort() + "/dataplane");
    private final URI dataPlane = URI.create("http://localhost:" + getFreePort());
    private final URI dataPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    private final URI dataPlanePublic = URI.create("http://localhost:" + getFreePort() + "/public");
    private final URI backendService = URI.create("http://localhost:" + getFreePort());
    private final URI idsEndpoint = URI.create("http://localhost:" + getFreePort());
    private final String name;

    public Participant(String name) {
        this.name = name;
    }

    private final ApiClient controlPlaneDataplaneClient = ApiClientFactory.createApiClient(controlPlaneDataplane.toString());
    private final ApiClient dataManagementClient = ApiClientFactory.createApiClient(controlPlane + "/api");
    private final DataplaneSelectorApi dataplaneSelectorApi = new DataplaneSelectorApi(controlPlaneDataplaneClient);
    private final AssetApi assetApi = new AssetApi(dataManagementClient);
    private final PolicyApi policyApi = new PolicyApi(dataManagementClient);
    private final CatalogApi catalogApi = new CatalogApi(dataManagementClient);
    private final ContractDefinitionApi contractDefinitionApi = new ContractDefinitionApi(dataManagementClient);
    private final ContractNegotiationApi contractNegotiationApi = new ContractNegotiationApi(dataManagementClient);
    private final TransferProcessApi transferProcessApi = new TransferProcessApi(dataManagementClient);

    public void createAsset(String assetId) {
        AssetEntryDto dto = new AssetEntryDto()
                .asset(new AssetDto().properties(Map.of(
                        "asset:prop:id", assetId,
                        "asset:prop:description", "description"
                )))
                .dataAddress(new DataAddressDto()
                        .properties(Map.of(
                                "name", "data",
                                "endpoint", backendService + "/api/service",
                                "type", "HttpData"
                        )));
        assetApi.createAsset(dto);
    }

    public String createPolicy(String assetId) {
        var policy = new Policy()
                .uid(UUID.randomUUID().toString())
                .addPermissionsItem(new Permission()
                        .target(assetId)
                        .action(new Action().type("USE")))
                .atType(Policy.AtTypeEnum.SET);

        policyApi.createPolicy(policy);

        return policy.getUid();
    }

    public void createContractDefinition(String policyId, String assetId, String definitionId) {
        var contractDefinition = new ContractDefinitionDto()
                .id(definitionId)
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .addCriteriaItem(
                        new Criterion()
                                .left("asset:prop:id")
                                .op("=")
                                .right(assetId));

        contractDefinitionApi.createContractDefinition(contractDefinition);
    }

    public String negotiateContract(Participant provider, ContractOffer contractOffer) {
        var request = new NegotiationInitiateRequestDto()
                .connectorId("provider")
                .connectorAddress(provider.idsEndpoint() + "/api/v1/ids/data")
                .protocol("ids-multipart")
                .offer(new ContractOfferDescription()
                        .offerId(contractOffer.getId())
                        .assetId(getAssetId(contractOffer))
                        .policy(contractOffer.getPolicy())
                );
        return contractNegotiationApi.initiateContractNegotiation(request).getId();
    }

    public String getAssetId(ContractOffer contractOffer) {
        return (String) contractOffer.getAsset().getProperties().get(Asset.PROPERTY_ID);
    }

    public String getContractAgreementId(String negotiationId) {
        var contractAgreementId = new AtomicReference<String>();

        await().atMost(timeout).untilAsserted(() -> {
            var result = contractNegotiationApi.getNegotiation(negotiationId).getContractAgreementId();
            assertThat(result).isNotNull();
            contractAgreementId.set(result);
        });

        return contractAgreementId.get();
    }

    public String dataRequest(String contractAgreementId, String assetId, Participant provider, DataAddress dataAddress) {
        var request = new TransferRequestDto()
                .contractId(contractAgreementId)
                .assetId(assetId)
                .connectorId("provider")
                .connectorAddress(provider.idsEndpoint() + "/api/v1/ids/data")
                .protocol("ids-multipart")
                .dataDestination(dataAddress)
                .managedResources(false)
                .transferType(new TransferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                );

        return transferProcessApi.initiateTransfer(request).getId();
    }

    public String getTransferProcessState(String transferProcessId) {
        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .when()
                .get("/api/transferprocess/{id}/state", transferProcessId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("state");
    }

    public URI backendService() {
        return backendService;
    }

    public void registerDataPlane() {
        var body = new DataPlaneInstanceImpl()
                .addAllowedSourceTypesItem("HttpData")
                .addAllowedDestTypesItem("HttpData")
                .edctype("dataspaceconnector:dataplaneinstance")
                .id(UUID.randomUUID().toString())
                .url(dataPlaneControl + "/transfer");

        dataplaneSelectorApi.addEntry(body);
    }

    public Catalog getCatalog(URI provider) {
        return catalogApi.getCatalog(provider + IDS_PATH + "/data");
    }

    public URI idsEndpoint() {
        return idsEndpoint;
    }

    public Map<String, String> controlPlaneConfiguration() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(controlPlane.getPort()));
                put("web.http.path", "/api");
                put("web.http.ids.port", String.valueOf(idsEndpoint.getPort()));
                put("web.http.ids.path", IDS_PATH);
                put("web.http.dataplane.port", String.valueOf(controlPlaneDataplane.getPort()));
                put("web.http.dataplane.path", controlPlaneDataplane.getPath());
                put("web.http.validation.port", String.valueOf(controlPlaneValidation.getPort()));
                put("web.http.validation.path", controlPlaneValidation.getPath());
                put("edc.vault", resourceAbsolutePath("consumer-vault.properties"));
                put("edc.keystore", resourceAbsolutePath("certs/cert.pfx"));
                put("edc.keystore.password", "123456");
                put("ids.webhook.address", idsEndpoint.toString());
                put("edc.receiver.http.endpoint", backendService + "/api/service/pull");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "1");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.transfer.proxy.endpoint", dataPlanePublic.toString());

                put("edc.datasource.asset.name", "asset");
                put("edc.datasource.asset.url", jdbcUrl());
                put("edc.datasource.asset.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.asset.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.contractdefinition.name", "contractdefinition");
                put("edc.datasource.contractdefinition.url", jdbcUrl());
                put("edc.datasource.contractdefinition.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.contractdefinition.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.contractnegotiation.name", "contractnegotiation");
                put("edc.datasource.contractnegotiation.url", jdbcUrl());
                put("edc.datasource.contractnegotiation.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.contractnegotiation.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.policy.name", "policy");
                put("edc.datasource.policy.url", jdbcUrl());
                put("edc.datasource.policy.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.policy.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.transferprocess.name", "transferprocess");
                put("edc.datasource.transferprocess.url", jdbcUrl());
                put("edc.datasource.transferprocess.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.transferprocess.password", PostgresqlLocalInstance.PASSWORD);
            }
        };
    }

    @NotNull
    public String jdbcUrl() {
        return PostgresqlLocalInstance.JDBC_URL_PREFIX + name;
    }

    public Map<String, String> dataPlaneConfiguration() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(dataPlane.getPort()));
                put("web.http.path", "/api");
                put("web.http.public.port", String.valueOf(dataPlanePublic.getPort()));
                put("web.http.public.path", "/public");
                put("web.http.control.port", String.valueOf(dataPlaneControl.getPort()));
                put("web.http.control.path", dataPlaneControl.getPath());
                put("edc.controlplane.validation-endpoint", controlPlaneValidation + "/token");
            }
        };
    }

    public String getName() {
        return name;
    }

    @NotNull
    private String resourceAbsolutePath(String filename) {
        return System.getProperty("user.dir") + separator + "src" + separator + "test" + separator + "resources" + separator + filename;
    }
}
