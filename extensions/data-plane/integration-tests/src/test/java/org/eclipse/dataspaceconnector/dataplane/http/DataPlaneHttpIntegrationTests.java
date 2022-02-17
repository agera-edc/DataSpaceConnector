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

package org.eclipse.dataspaceconnector.dataplane.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.dataplane.http.schema.HttpDataSchema;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

/**
 * System Test for Data Plane HTTP extension.
 */
public class DataPlaneHttpIntegrationTests {

    private static final int DPF_API_PORT = getFreePort();
    private static final int DPF_HTTP_SOURCE_API_PORT = getFreePort();
    private static final int DPF_HTTP_SINK_API_PORT = getFreePort();
    private static final String DPF_API_HOST = "http://localhost:" + DPF_API_PORT;
    private static final String DPF_HTTP_SOURCE_API_HOST = "http://localhost:" + DPF_HTTP_SOURCE_API_PORT;
    private static final String DPF_HTTP_SINK_API_HOST = "http://localhost:" + DPF_HTTP_SINK_API_PORT;
    private static final String TRANSFER_PATH = "/api/transfer";
    private static final String EDC_TYPE = "edctype";
    private static final String DPF_HTTP_API_PART_NAME = "sample";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Faker faker = new Faker();

    /**
     * HTTP Source mock server.
     */
    private static ClientAndServer dpfHttpSourceClientAndServer;
    /**
     * HTTP Sink mock server.
     */
    private static ClientAndServer dpfHttpSinkClientAndServer;

    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":launchers:data-plane-server",
            "data-plane-server",
            Map.of("web.http.control.port", String.valueOf(DPF_API_PORT)));

    @BeforeAll
    public static void setUp() {
        dpfHttpSourceClientAndServer = startClientAndServer(DPF_HTTP_SOURCE_API_PORT);
        dpfHttpSinkClientAndServer = startClientAndServer(DPF_HTTP_SINK_API_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(dpfHttpSourceClientAndServer);
        stopQuietly(dpfHttpSinkClientAndServer);
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        dpfHttpSourceClientAndServer.reset();
        dpfHttpSinkClientAndServer.reset();
    }


    @Test
    public void transfer_success() {
        // Arrange

        // HTTP Source Request & Response
        var dpfSourceResponseBody = OBJECT_MAPPER.createObjectNode()
                .put("data", UUID.randomUUID().toString());
        dpfHttpSourceClientAndServer
                .when(
                        request()
                                .withMethod(HttpMethod.GET.name())
                                .withPath("/" + DPF_HTTP_API_PART_NAME),
                        once()
                )
                .respond(
                        response()
                                .withStatusCode(HttpStatusCode.OK_200.code())
                                .withHeader(
                                        new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                                MediaType.APPLICATION_JSON_UTF_8.toString())
                                )
                                .withBody(dpfSourceResponseBody.toString())
                );


        // HTTP Sink Request & Response
        dpfHttpSinkClientAndServer
                .when(
                        request()
                                .withMethod(HttpMethod.POST.name())
                                .withPath("/" + DPF_HTTP_API_PART_NAME)
                                .withHeader(
                                        new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                                MediaType.APPLICATION_OCTET_STREAM.toString())
                                )
                                .withBody(binary(dpfSourceResponseBody.toString().getBytes(StandardCharsets.UTF_8))),
                        once()
                )
                .respond(
                        response()
                                .withStatusCode(HttpStatusCode.OK_200.code())
                );

        // Act & Assert

        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(validDataFlowRequest())
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);

        // Verify HTTP Source server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                dpfHttpSourceClientAndServer
                        .verify(
                                request()
                                        .withMethod(HttpMethod.GET.name())
                                        .withPath("/" + DPF_HTTP_API_PART_NAME),
                                VerificationTimes.once()
                        )
        );

        // Verify HTTP Sink server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                dpfHttpSinkClientAndServer
                        .verify(
                                request()
                                        .withMethod(HttpMethod.POST.name())
                                        .withPath("/" + DPF_HTTP_API_PART_NAME)
                                        .withHeader(
                                                new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                                        MediaType.APPLICATION_OCTET_STREAM.toString())
                                        )
                                        .withBody(binary(dpfSourceResponseBody.toString().getBytes(StandardCharsets.UTF_8))),
                                VerificationTimes.once()
                        )
        );

    }

    /**
     * Test to verify DPF transfer api layer validation is working as expected.
     */
    @Test
    public void transfer_invalidInput_failure() {
        // Arrange
        // Request with invalid source and destination type to initiate transfer.
        var invalidRequest = DataFlowRequest.Builder.newInstance()
                .id(faker.internet().uuid())
                .processId(faker.internet().uuid())
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(faker.lorem().word())
                        .properties(Map.of(
                                HttpDataSchema.ENDPOINT, DPF_HTTP_SOURCE_API_HOST,
                                HttpDataSchema.NAME, DPF_HTTP_API_PART_NAME
                        ))
                        .build())
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(faker.lorem().word())
                        .properties(Map.of(
                                HttpDataSchema.ENDPOINT, DPF_HTTP_SINK_API_HOST
                        ))
                        .build())
                .build();

        // Act & Assert
        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

    }

    @Test
    public void transfer_sourceNotAvailable_noInteractionWithSink() {
        // Arrange

        // HTTP Source Request & Error Response
        dpfHttpSourceClientAndServer
                .when(
                        request()
                                .withMethod(HttpMethod.GET.name())
                                .withPath("/" + DPF_HTTP_API_PART_NAME)
                )
                .error(
                        error()
                                .withDropConnection(true)
                );

        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(validDataFlowRequest())
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);

        // Verify HTTP Source server called at lest once.
        await().atMost(30, SECONDS).untilAsserted(() ->
                dpfHttpSourceClientAndServer
                        .verify(
                                request()
                                        .withMethod(HttpMethod.GET.name())
                                        .withPath("/" + DPF_HTTP_API_PART_NAME),
                                VerificationTimes.atLeast(1)
                        )
        );

        // Verify zero interaction with HTTP Sink.
        dpfHttpSinkClientAndServer.verifyZeroInteractions();
    }

    /**
     * Validate if source is unavailable intermittently than DPF server retries to fetch data.
     */
    @Test
    public void transfer_sourceTemporaryFailure_success() {
        // Arrange
        // First two calls to HTTP Source returns a failure response.
        dpfHttpSourceClientAndServer
                .when(
                        request()
                                .withMethod(HttpMethod.GET.name())
                                .withPath("/" + DPF_HTTP_API_PART_NAME),
                        exactly(2)
                )
                .error(
                        error()
                                .withDropConnection(true)
                );

        // Next call to HTTP Source returns a valid response.
        var dpfSourceResponseBody = OBJECT_MAPPER.createObjectNode()
                .put("data", UUID.randomUUID().toString());
        dpfHttpSourceClientAndServer
                .when(
                        request()
                                .withMethod(HttpMethod.GET.name())
                                .withPath("/" + DPF_HTTP_API_PART_NAME),
                        once()
                )
                .respond(
                        response()
                                .withStatusCode(HttpStatusCode.OK_200.code())
                                .withHeader(
                                        new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                                MediaType.APPLICATION_JSON_UTF_8.toString())
                                )
                                .withBody(dpfSourceResponseBody.toString())
                );

        // Act & Assert

        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(validDataFlowRequest())
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);

        // Verify HTTP Source server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                dpfHttpSourceClientAndServer
                        .verify(
                                request()
                                        .withMethod(HttpMethod.GET.name())
                                        .withPath("/" + DPF_HTTP_API_PART_NAME),
                                VerificationTimes.exactly(3)
                        )
        );

        // Verify HTTP Sink server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                dpfHttpSinkClientAndServer
                        .verify(
                                request()
                                        .withMethod(HttpMethod.POST.name())
                                        .withPath("/" + DPF_HTTP_API_PART_NAME)
                                        .withHeader(
                                                new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                                        MediaType.APPLICATION_OCTET_STREAM.toString())
                                        )
                                        .withBody(binary(dpfSourceResponseBody.toString().getBytes(StandardCharsets.UTF_8))),
                                VerificationTimes.once()
                        )
        );
    }

    private ObjectNode validDataFlowRequest() {
        // Create valid dataflow request instance.
        var request = DataFlowRequest.Builder.newInstance()
                .id(faker.internet().uuid())
                .processId(faker.internet().uuid())
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(HttpDataSchema.TYPE)
                        .properties(Map.of(
                                HttpDataSchema.ENDPOINT, DPF_HTTP_SOURCE_API_HOST,
                                HttpDataSchema.NAME, DPF_HTTP_API_PART_NAME
                        ))
                        .build())
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(HttpDataSchema.TYPE)
                        .properties(Map.of(
                                HttpDataSchema.ENDPOINT, DPF_HTTP_SINK_API_HOST
                        ))
                        .build())
                .build();

        // Add edctype to request
        var requestJsonNode = OBJECT_MAPPER.convertValue(request, ObjectNode.class);
        requestJsonNode.put(EDC_TYPE, "dataspaceconnector:dataflowrequest");

        return requestJsonNode;
    }

    private RequestSpecification givenDpfRequest() {
        return given()
                .baseUri(DPF_API_HOST);
    }
}
