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

import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;

// @EndToEndTest
class IdsDidAuthTest {
    public static final int CONSUMER_CONNECTOR_PORT = getFreePort();
    public static final int CONSUMER_MANAGEMENT_PORT = getFreePort();
    public static final String CONSUMER_CONNECTOR_PATH = "/api";
    public static final String CONSUMER_MANAGEMENT_PATH = "/api/v1/data";
    public static final String CONSUMER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + CONSUMER_MANAGEMENT_PORT;
    public static final int CONSUMER_IDS_API_PORT = getFreePort();
    public static final String CONSUMER_IDS_API = "http://localhost:" + CONSUMER_IDS_API_PORT;

    public static final int PROVIDER_CONNECTOR_PORT = getFreePort();
    public static final int PROVIDER_MANAGEMENT_PORT = getFreePort();
    public static final String PROVIDER_CONNECTOR_PATH = "/api";
    public static final String PROVIDER_MANAGEMENT_PATH = "/api/v1/data";
    public static final String PROVIDER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + PROVIDER_MANAGEMENT_PORT;
    public static final int PROVIDER_IDS_API_PORT = getFreePort();
    public static final String PROVIDER_IDS_API = "http://localhost:" + PROVIDER_IDS_API_PORT;

    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:ids-did-auth-test:consumer",
            "consumer",
            Map.ofEntries(
                    entry("edc.identity.did.url", "did:web:consumer"),
                    // entry("edc.keystore", getFileFromResourceName("provider-keystore.jks").getPath()),
                    // entry("edc.vault", getFileFromResourceName("provider-vault.properties").getPath()),
                    // entry("edc.keystore.password", "test123"),
                    entry("edc.connector.name", "test-connector-name"),
                    entry("web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT)),
                    entry("web.http.path", CONSUMER_CONNECTOR_PATH),
                    entry("web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT)),
                    entry("web.http.data.path", CONSUMER_MANAGEMENT_PATH),
                    entry("web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT)),
                    entry("web.http.ids.path", "/api/v1/ids"),
                    entry("ids.webhook.address", CONSUMER_IDS_API)));

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:ids-did-auth-test:provider",
            "provider",
            Map.ofEntries(
                    entry("edc.identity.did.url", "did:web:provider"),
                    // entry("edc.keystore", getFileFromResourceName("provider-keystore.jks").getPath()),
                    // entry("edc.vault", getFileFromResourceName("provider-vault.properties").getPath()),
                    // entry("edc.keystore.password", "test123"),
                    entry("edc.connector.name", "test-connector-name"),
                    entry("web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT)),
                    entry("web.http.path", PROVIDER_CONNECTOR_PATH),
                    entry("web.http.data.port", String.valueOf(PROVIDER_MANAGEMENT_PORT)),
                    entry("web.http.data.path", PROVIDER_MANAGEMENT_PATH),
                    entry("web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT)),
                    entry("web.http.ids.path", "/api/v1/ids"),
                    entry("ids.webhook.address", PROVIDER_IDS_API)));

    static final IdsDidAuthParticipant CONSUMER = new IdsDidAuthParticipant(CONSUMER_CONNECTOR_MANAGEMENT_URL + CONSUMER_MANAGEMENT_PATH, CONSUMER_IDS_API);
    static final IdsDidAuthParticipant PROVIDER = new IdsDidAuthParticipant(PROVIDER_CONNECTOR_MANAGEMENT_URL + PROVIDER_MANAGEMENT_PATH, PROVIDER_IDS_API);

    @BeforeAll
    public static void setUp() {
        // Request{method=GET, url=https://consumer/.well-known/did.json}
    }

    @AfterAll
    public static void tearDown() {
    }

    @Test
    void dataTransfer() {

        createAssetAndContractDefinitionOnProvider();

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSize(1);
    }

    private void createAssetAndContractDefinitionOnProvider() {
        var assetId = "asset-id";
        PROVIDER.createAsset(assetId);
        var policyId = PROVIDER.createPolicy(assetId);
        PROVIDER.createContractDefinition(policyId);
    }
}
