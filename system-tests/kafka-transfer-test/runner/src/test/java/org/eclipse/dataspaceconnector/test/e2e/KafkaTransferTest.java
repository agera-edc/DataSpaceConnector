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

import io.netty.handler.codec.http.HttpMethod;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.dataspaceconnector.dataplane.kafka.models.KafkaRecordsDto;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.tempDirectory;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.test.e2e.KafkaParticipant.KAFKA_SERVER;
import static org.eclipse.dataspaceconnector.test.e2e.KafkaParticipant.KAFKA_TOPIC;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.stop.Stop.stopQuietly;
import static org.mockserver.verify.VerificationTimes.atLeast;

// @EndToEndTest
class KafkaTransferTest {
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

    public static final String PROVIDER_ASSET_FILE = "text-document.txt";
    public static final String PROVIDER_ASSET_PATH = format("%s/%s.txt", tempDirectory(), PROVIDER_ASSET_FILE);

    private static final int EVENT_DESTINATION_PORT = getFreePort();
    private static ClientAndServer eventDestination;
    private static ScheduledExecutorService executor;

    private final Duration timeout = Duration.ofMinutes(5);

    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:kafka-transfer-test:consumer",
            "consumer",
            Map.of(
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "web.http.path", CONSUMER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
                    "web.http.data.path", CONSUMER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
                    "web.http.ids.path", "/api/v1/ids",
                    "ids.webhook.address", CONSUMER_IDS_API));

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:kafka-transfer-test:provider",
            "provider",
            Map.of(
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "edc.test.asset.path", PROVIDER_ASSET_PATH,
                    "web.http.path", PROVIDER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(PROVIDER_MANAGEMENT_PORT),
                    "web.http.data.path", PROVIDER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT),
                    "web.http.ids.path", "/api/v1/ids",
                    "edc.samples.04.asset.path", PROVIDER_ASSET_PATH,
                    "ids.webhook.address", PROVIDER_IDS_API));

    static final KafkaParticipant CONSUMER = new KafkaParticipant(CONSUMER_CONNECTOR_MANAGEMENT_URL + CONSUMER_MANAGEMENT_PATH, CONSUMER_IDS_API);
    static final KafkaParticipant PROVIDER = new KafkaParticipant(PROVIDER_CONNECTOR_MANAGEMENT_URL + PROVIDER_MANAGEMENT_PATH, PROVIDER_IDS_API);
    static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger();

    @BeforeAll
    public static void setUp() {
        eventDestination = startClientAndServer(EVENT_DESTINATION_PORT);
        startKafkaProducer();
    }

    private static void startKafkaProducer() {
        var producer = createKafkaProducer();
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(
                () -> producer.send(new ProducerRecord<>(KAFKA_TOPIC, null, format("{\"id\":%d,\"name\":\"joe\"}", MESSAGE_COUNTER.incrementAndGet()))),
                0, 100, TimeUnit.MILLISECONDS);
    }

    private static Producer<String, String> createKafkaProducer() {
        var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(eventDestination);
        executor.shutdown();
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        eventDestination.reset();
    }

    @Test
    void dataTransfer() {

        createAssetAndContractDefinitionOnProvider();

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSize(1);

        var assetId = catalog.getContractOffers().get(0).getAsset().getId();
        var negotiationId = CONSUMER.negotiateContract(assetId, PROVIDER);
        var contractAgreementId = CONSUMER.getContractAgreementId(negotiationId);

        assertThat(contractAgreementId).isNotEmpty();

        var transferProcessId = CONSUMER.dataRequest(contractAgreementId, assetId, PROVIDER, server());

        await().atMost(timeout).pollDelay(Duration.ofSeconds(1)).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });

        HttpRequest requestDefinition = request()
                .withMethod(HttpMethod.POST.name())
                .withPath("/api/service/" + KAFKA_TOPIC);
        await().atMost(timeout).untilAsserted(() -> eventDestination.verify(requestDefinition, atLeast(1)));

        var request = eventDestination.retrieveRecordedRequests(requestDefinition);
        var httpRequest = Arrays.stream(request).findFirst().orElseThrow();
        var payloadBase64 = httpRequest.getBodyAsJsonOrXmlString();
        var decoded = Base64.getDecoder().decode(payloadBase64);
        assertThat(decoded).isNotEmpty();
        var dto = new TypeManager().readValue(decoded, KafkaRecordsDto.class);
        assertThat(dto.getTopic()).isEqualTo(KAFKA_TOPIC);
        assertThat(dto.getRecords()).isNotEmpty();
        var value = dto.getRecords().get(0).getValue();
        assertThat(value)
                .isInstanceOfSatisfying(Map.class,
                        m -> assertThat(m).containsEntry("name", "joe"));
    }

    private void createAssetAndContractDefinitionOnProvider() {
        var assetId = "asset-id";
        PROVIDER.createAsset(assetId);
        var policyId = PROVIDER.createPolicy(assetId);
        PROVIDER.createContractDefinition(policyId);
    }

    private DataAddress server() {
        return DataAddress.Builder.newInstance()
                .type("HttpData")
                .property(NAME, "data")
                .property(ENDPOINT, format("http://localhost:%s/api/service", EVENT_DESTINATION_PORT))
                .build();
    }

}
