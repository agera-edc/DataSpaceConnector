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
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.system.tests.local;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.proto.trace.v1.Span;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.junit.launcher.OpenTelemetryExtension;
import org.eclipse.dataspaceconnector.opentelemetry.OpenTelemetryIntegrationTest;
import org.eclipse.dataspaceconnector.opentelemetry.OtlpGrpcServer;
import org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.API_KEY_CONTROL_AUTH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_ASSET_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;

@OpenTelemetryIntegrationTest
@ExtendWith(OpenTelemetryExtension.class)
public class TracingIntegrationTests {

    static OtlpGrpcServer grpcServer;

    String[] contractNegotiationSpanNames = new String[] {
            "ConsumerContractNegotiationManagerImpl.initiate",
            "ProviderContractNegotiationManagerImpl.requested",
            "ConsumerContractNegotiationManagerImpl.confirmed"
    };

    String[] transferProcessSpanNames = new String[] {
            "TransferProcessManagerImpl.initiateConsumerRequest",
            "TransferProcessManagerImpl.processInitial",
            "TransferProcessManagerImpl.processProvisioned",
            "TransferProcessManagerImpl.initiateProviderRequest"
    };

    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:file-transfer-consumer",
            "consumer",
            Map.of(
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "web.http.path", CONSUMER_CONNECTOR_PATH,
                    "web.http.data.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
                    "web.http.data.path", CONSUMER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
                    "web.http.ids.path", "/api/v1/ids",
                    "edc.api.control.auth.apikey.value", API_KEY_CONTROL_AUTH,
                    "ids.webhook.address", CONSUMER_IDS_API));

    @RegisterExtension
    static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:file-transfer-provider",
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

    @BeforeAll
    static void startGrpcServer() {
        grpcServer = new OtlpGrpcServer();
        grpcServer.start();
    }

    @AfterAll
    static void stopGrpcServer() {
        grpcServer.stop().join();
    }

    @BeforeEach
    void resetGrpcServer() {
        grpcServer.reset();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void transferFile_testTraces() throws Exception {
        // Arrange
        // Create a file with test data on provider file system.
        String fileContent = "FileTransfer-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));

        // Act
        runGatling(FileTransferLocalSimulation.class, FileTransferSimulationUtils.DESCRIPTION);

        // Assert
        await().atMost(30, SECONDS).untilAsserted(() -> {
                    // Get exported spans.
                    List<Span> spans = grpcServer
                            .traceRequests
                            .stream()
                            .flatMap(r -> r.getResourceSpansList().stream())
                            .flatMap(r -> r.getInstrumentationLibrarySpansList().stream())
                            .flatMap(r -> r.getSpansList().stream())
                            .collect(Collectors.toList());

                    // Assert that expected spans are present.
                    List<Span> contractNegotiationSpans = getSpans(spans, Arrays.stream(contractNegotiationSpanNames));
                    List<Span> transferProcessSpans = getSpans(spans, Arrays.stream(transferProcessSpanNames));

                    for (Span span: contractNegotiationSpans) {
                        System.out.println(span.getName() + " " + span.getTraceId());
                    }

                    System.out.println(" ");
                    for (Span span: transferProcessSpans) {
                        System.out.println(span.getName() + " " + span.getTraceId());
                    }
                    // Assert that spans are part of the right trace.
                    assertSpansHaveSameTrace(contractNegotiationSpans);
                    assertSpansHaveSameTrace(transferProcessSpans);
                }
        );
    }

    private List<Span> getSpans(List<Span> spans, Stream<String> spanNames) {
        return spanNames.map(spanName -> getSpanByName(spans, spanName)).collect(Collectors.toList());
    }

    private void assertSpansHaveSameTrace(List<Span> spans) {
        assertThat(spans.stream().map(s -> s.getTraceId().toStringUtf8()).distinct())
                .withFailMessage(() -> "Spans from the same trace should have the same traceId.")
                .singleElement();
    }

    private Span getSpanByName(Collection<Span> spans, String name) {
        var span = spans.stream().filter(s -> name.equals(s.getName())).findFirst();
        assertThat(span)
                .withFailMessage(format("Span %s is missing", name))
                .isPresent();
        return span.get();
    }
}
