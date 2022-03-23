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

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.trace.v1.Span;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.OpenTelemetryIntegrationTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
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
public class TracingIntegrationTests {

    String[] contractNegotiationSpanNames = new String[] {
            "ConsumerContractNegotiationManagerImpl.initiate",
            "ProviderContractNegotiationManagerImpl.requested",
            "ConsumerContractNegotiationManagerImpl.confirmed"
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

    static OtlpGrpcServer grpcServer;

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
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        assertThat(runtimeMxBean.getInputArguments())
                .withFailMessage("OpenTelemetry Agent JAR should be present. See README.md file for details.")
                .anyMatch(arg -> arg.startsWith("-javaagent"));

        // Arrange
        // Create a file with test data on provider file system.
        var fileContent = "FileTransfer-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));

        // Act
        runGatling(FileTransferLocalSimulation.class, FileTransferSimulationUtils.DESCRIPTION);

        // Assert
        await().atMost(30, SECONDS).untilAsserted(() -> {
                    var spans = grpcServer
                            .traceRequests
                            .stream()
                            .flatMap(r -> r.getResourceSpansList().stream())
                            .flatMap(r -> r.getInstrumentationLibrarySpansList().stream())
                            .flatMap(r -> r.getSpansList().stream())
                            .collect(Collectors.toList());

                    List<Span> contractNegotationSpans = Arrays.stream(contractNegotiationSpanNames)
                            .map(spanName -> getSpanByName(spans, spanName)).collect(Collectors.toList());

                    // Make sure that each span have the same traceId.
                    contractNegotationSpans.stream().reduce((span1, span2) -> {
                        assertThat(span1.getTraceId())
                                .withFailMessage(format("Span %s should have the same traceId as span %s. They should be part of the same trace", span1.getName(), span2.getName()))
                                .isEqualTo(span2.getTraceId());
                        return span2;
                    }).isPresent();
                }
        );
    }

    private Span getSpanByName(Collection<Span> spans, String name) {
        var span = spans.stream().filter(s -> name.equals(s.getName())).findFirst();
        assertThat(span).isPresent();
        return span.get();
    }

    // Class modeled on https://github.com/open-telemetry/opentelemetry-java/blob/338966e4786c027afdffa29ea9cc233ea0360409/integration-tests/otlp/src/main/java/io/opentelemetry/integrationtest/OtlpExporterIntegrationTest.java
    private static class OtlpGrpcServer extends ServerExtension {

        private final List<ExportTraceServiceRequest> traceRequests = new ArrayList<>();

        private void reset() {
            traceRequests.clear();
        }

        @Override
        protected void configure(ServerBuilder sb) {
            sb.http(4317); // default GRPC port https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#otlp-exporter-both-span-and-metric-exporters

            sb.service(
                    "/opentelemetry.proto.collector.trace.v1.TraceService/Export",
                    new AbstractUnaryGrpcService() {
                        @Override
                        protected @NotNull CompletionStage<byte[]> handleMessage(
                                @NotNull ServiceRequestContext ctx, byte @NotNull [] message) {
                            try {
                                traceRequests.add(ExportTraceServiceRequest.parseFrom(message));
                            } catch (InvalidProtocolBufferException e) {
                                throw new UncheckedIOException(e);
                            }
                            return completedFuture(ExportTraceServiceResponse.getDefaultInstance().toByteArray());
                        }
                    });
        }
    }
}
