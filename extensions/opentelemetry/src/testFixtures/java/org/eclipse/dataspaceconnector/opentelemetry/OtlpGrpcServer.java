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

package org.eclipse.dataspaceconnector.opentelemetry;

import org.jetbrains.annotations.NotNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

// Class modeled on https://github.com/open-telemetry/opentelemetry-java/blob/338966e4786c027afdffa29ea9cc233ea0360409/integration-tests/otlp/src/main/java/io/opentelemetry/integrationtest/OtlpExporterIntegrationTest.java
public class OtlpGrpcServer extends ServerExtension {

    public final List<ExportTraceServiceRequest> traceRequests = new ArrayList<>();

    public void reset() {
        traceRequests.clear();
    }

    @Override
    protected void configure(ServerBuilder sb) {
        sb.http(4317); // Default GRPC port https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#otlp-exporter-both-span-and-metric-exporters

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