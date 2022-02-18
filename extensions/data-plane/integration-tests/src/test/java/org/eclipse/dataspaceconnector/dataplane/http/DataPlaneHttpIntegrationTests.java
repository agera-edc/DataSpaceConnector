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
import org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.schema.HttpDataSchema;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

/**
 * System Test for Data Plane HTTP extension.
 */
public class DataPlaneHttpIntegrationTests {

    private static final int DPF_PUBLIC_API_PORT = getFreePort();
    private static final int DPF_CONTROL_API_PORT = getFreePort();
    private static final int DPF_HTTP_SOURCE_API_PORT = getFreePort();
    private static final int DPF_HTTP_SINK_API_PORT = getFreePort();
    private static final String DPF_CONTROL_API_HOST = "http://localhost:" + DPF_CONTROL_API_PORT;
    private static final String DPF_HTTP_SOURCE_API_HOST = "http://localhost:" + DPF_HTTP_SOURCE_API_PORT;
    private static final String DPF_HTTP_SINK_API_HOST = "http://localhost:" + DPF_HTTP_SINK_API_PORT;
    private static final String CONTROL_PATH = "/control";
    private static final String TRANSFER_PATH = format("%s/transfer", CONTROL_PATH);
    private static final String EDC_TYPE = "edctype";
    private static final String DATA_FLOW_REQUEST_EDC_TYPE = "dataspaceconnector:dataflowrequest";
    private static final String DPF_HTTP_API_PART_NAME = "sample";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Faker faker = new Faker();

    /**
     * HTTP Source mock server.
     */
    private static ClientAndServer httpSourceClientAndServer;
    /**
     * HTTP Sink mock server.
     */
    private static ClientAndServer httpSinkClientAndServer;

    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":launchers:data-plane-server",
            "data-plane-server",
            Map.of(
                    "web.http.public.port", String.valueOf(DPF_PUBLIC_API_PORT),
                    "web.http.control.port", String.valueOf(DPF_CONTROL_API_PORT),
                    "web.http.control.path", CONTROL_PATH
            ));

    @BeforeAll
    public static void setUp() {
        httpSourceClientAndServer = startClientAndServer(DPF_HTTP_SOURCE_API_PORT);
        httpSinkClientAndServer = startClientAndServer(DPF_HTTP_SINK_API_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(httpSourceClientAndServer);
        stopQuietly(httpSinkClientAndServer);
    }

    /**
     * Reset mock server internal state after every test.
     */
    @AfterEach
    public void resetMockServer() {
        httpSourceClientAndServer.reset();
        httpSinkClientAndServer.reset();
    }

    @Test
    public void transfer_success() {
        // Arrange
        // HTTP Source Request & Response
        var body = UUID.randomUUID().toString();
        httpSourceClientAndServer
                .when(
                        givenGetRequest(),
                        once()
                )
                .respond(
                        withResponse(HttpStatusCode.OK_200, body)
                );

        // HTTP Sink Request & Response
        httpSinkClientAndServer
                .when(
                        givenPostRequest(body),
                        once()
                )
                .respond(
                        withResponse(HttpStatusCode.OK_200)
                );

        // Act & Assert
        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(transferRequestBody())
                .when()
                .log()
                .all()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
        // TODO: Assertions can be a race-condition. Need a dpf api to check transfer process is completed.
        // Verify HTTP Source server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                httpSourceClientAndServer
                        .verify(
                                givenGetRequest(),
                                VerificationTimes.once()
                        )
        );

        // Verify HTTP Sink server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                httpSinkClientAndServer
                        .verify(
                                givenPostRequest(body),
                                VerificationTimes.once()
                        )
        );
    }

    /**
     * Verify DPF transfer api layer validation is working as expected.
     */
    @Test
    public void transfer_invalidInput_failure() {
        // Arrange
        // Request without processId to initiate transfer.
        var invalidRequest = transferRequestBody().remove("processId");

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
        httpSourceClientAndServer
                .when(
                        givenGetRequest()
                )
                .error(
                        withDropConnection()
                );

        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(transferRequestBody())
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);

        // TODO: Assertions can be a race-condition. Need a dpf api to check transfer process is completed.
        // Verify HTTP Source server called at lest once.
        await().atMost(30, SECONDS).untilAsserted(() ->
                httpSourceClientAndServer
                        .verify(
                                givenGetRequest(),
                                VerificationTimes.atLeast(1)
                        )
        );

        // Verify zero interaction with HTTP Sink.
        httpSinkClientAndServer.verifyZeroInteractions();
    }

    /**
     * Validate if intermittently source is dropping connection than DPF server retries to fetch data.
     */
    @Test
    public void transfer_sourceTemporaryDropConnection_success() {
        // Arrange
        // First two calls to HTTP Source returns a failure response.
        httpSourceClientAndServer
                .when(
                        givenGetRequest(),
                        exactly(2)
                )
                .error(
                        withDropConnection()
                );

        // Next call to HTTP Source returns a valid response.
        var body = UUID.randomUUID().toString();
        httpSourceClientAndServer
                .when(
                        givenGetRequest(),
                        once()
                )
                .respond(
                        withResponse(HttpStatusCode.OK_200, body)
                );

        // HTTP Sink Request & Response
        httpSinkClientAndServer
                .when(
                        givenPostRequest(body),
                        once()
                )
                .respond(
                        withResponse(HttpStatusCode.OK_200)
                );

        // Act & Assert
        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(transferRequestBody())
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
        // TODO: Assertions can be a race-condition. Need a dpf api to check transfer process is completed.
        // Verify HTTP Source server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                httpSourceClientAndServer
                        .verify(
                                givenGetRequest(),
                                VerificationTimes.exactly(3)
                        )
        );

        // Verify HTTP Sink server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                httpSinkClientAndServer
                        .verify(
                                givenPostRequest(body),
                                VerificationTimes.once()
                        )
        );
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideCommonErrorCodes")
    public void transfer_sourceErrorResponse_failure(String name, HttpStatusCode httpStatusCode) {
        // Arrange
        // HTTP Source returns error response.
        httpSourceClientAndServer
                .when(
                        givenGetRequest(),
                        once()
                )
                .respond(
                        withResponse(httpStatusCode)
                );

        // Act & Assert
        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(transferRequestBody())
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
        // TODO: Assertions can be a race-condition. Need a dpf api to check transfer process is completed.
        // Verify HTTP Source server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                httpSourceClientAndServer
                        .verify(
                                givenGetRequest(),
                                VerificationTimes.once()
                        )
        );

        httpSinkClientAndServer.verifyZeroInteractions();
    }

    /**
     * Validate if sink is dropping connection than DPF server retries to push data.
     */
    @Test
    @Disabled
    public void transfer_sinkNotAvailable_success() {
        // Arrange
        // HTTP Source Request & Response
        var body = UUID.randomUUID().toString();
        httpSourceClientAndServer
                .when(
                        givenGetRequest(),
                        once()
                )
                .respond(
                        withResponse(HttpStatusCode.OK_200, body)
                );

        // First two calls to HTTP sink returns a failure response.
        httpSinkClientAndServer
                .when(
                        givenPostRequest(body),
                        exactly(2)
                )
                .error(
                        withDropConnection()
                );

        // Next call to HTTP sink returns a valid response.
        httpSinkClientAndServer
                .when(
                        givenPostRequest(body),
                        once()
                )
                .respond(
                        withResponse(HttpStatusCode.OK_200)
                );

        // Act & Assert
        // Initiate transfer
        givenDpfRequest()
                .contentType(ContentType.JSON)
                .body(transferRequestBody())
                .when()
                .post(TRANSFER_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
        // TODO: Assertions can be a race-condition. Need a dpf api to check transfer process is completed.
        // Verify HTTP Source server expectation.
        await().atMost(30, SECONDS).untilAsserted(() ->
                httpSourceClientAndServer
                        .verify(
                                givenGetRequest(),
                                VerificationTimes.once()
                        )
        );

        // Verify HTTP Sink server expectation.
        await().atMost(10, SECONDS).untilAsserted(() ->
                httpSinkClientAndServer
                        .verify(
                                givenPostRequest(body),
                                VerificationTimes.exactly(3)
                        )
        );
    }

    /**
     * Request payload to initiate DPF transfer.
     *
     * @return JSON object. see {@link ObjectNode}.
     */
    private ObjectNode transferRequestBody() {
        // Create valid dataflow request instance.
        var request = DataFlowRequest.Builder.newInstance()
                .id(faker.internet().uuid())
                .processId(faker.internet().uuid())
                .properties(Map.of(DataFlowRequestSchema.METHOD, HttpMethod.GET.name()))
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
        requestJsonNode.put(EDC_TYPE, DATA_FLOW_REQUEST_EDC_TYPE);

        return requestJsonNode;
    }

    /**
     * RestAssured request specification with DPF API host as base URI.
     *
     * @return see {@link RequestSpecification}
     */
    private RequestSpecification givenDpfRequest() {
        return given()
                .baseUri(DPF_CONTROL_API_HOST);
    }

    /**
     * Mock HTTP GET request for source.
     *
     * @return see {@link HttpRequest}
     */
    private HttpRequest givenGetRequest() {
        return request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/" + DPF_HTTP_API_PART_NAME);
    }

    /**
     * Mock plain text response from source.
     *
     * @param statusCode   Response status code.
     * @param responseBody Response body.
     * @return see {@link HttpResponse}
     */
    private HttpResponse withResponse(HttpStatusCode statusCode, String responseBody) {
        var response = response()
                .withStatusCode(statusCode.code());

        if (responseBody != null) {
            response.withHeader(
                            new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                    MediaType.PLAIN_TEXT_UTF_8.toString())
                    )
                    .withBody(responseBody);
        }

        return response;
    }

    /**
     * Mock HTTP POST request for sink.
     *
     * @param responseBody Request body.
     * @return see {@link HttpRequest}
     */
    private HttpRequest givenPostRequest(String responseBody) {
        return request()
                .withMethod(HttpMethod.POST.name())
                .withPath("/" + DPF_HTTP_API_PART_NAME)
                .withHeader(
                        new Header(HttpHeaderNames.CONTENT_TYPE.toString(),
                                MediaType.APPLICATION_OCTET_STREAM.toString())
                )
                .withBody(binary(responseBody.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Mock response from sink.
     *
     * @param statusCode Response status code.
     * @return see {@link HttpResponse}
     */
    private HttpResponse withResponse(HttpStatusCode statusCode) {
        return withResponse(statusCode, null);
    }

    /**
     * Mock error response which to force the connection to be dropped without any response being returned.
     *
     * @return see {@link HttpError}
     */
    private HttpError withDropConnection() {
        return error()
                .withDropConnection(true);
    }

    /**
     * Provides most common http error status codes.
     *
     * @return Http Error codes as {@link Stream} of {@link Arguments}.
     */
    private static Stream<Arguments> provideCommonErrorCodes() {
        return Stream.of(
                Arguments.of(HttpStatusCode.MOVED_PERMANENTLY_301.name(), HttpStatusCode.MOVED_PERMANENTLY_301),
                Arguments.of(HttpStatusCode.FOUND_302.name(), HttpStatusCode.FOUND_302),
                Arguments.of(HttpStatusCode.BAD_REQUEST_400.name(), HttpStatusCode.BAD_REQUEST_400),
                Arguments.of(HttpStatusCode.UNAUTHORIZED_401.name(), HttpStatusCode.UNAUTHORIZED_401),
                Arguments.of(HttpStatusCode.NOT_FOUND_404.name(), HttpStatusCode.NOT_FOUND_404),
                Arguments.of(HttpStatusCode.INTERNAL_SERVER_ERROR_500.name(), HttpStatusCode.INTERNAL_SERVER_ERROR_500),
                Arguments.of(HttpStatusCode.BAD_GATEWAY_502.name(), HttpStatusCode.BAD_GATEWAY_502)
        );
    }

}
