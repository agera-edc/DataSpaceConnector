/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;

public class DataFlowRequestFactory {

    private final TypeManager typeManager;

    public DataFlowRequestFactory(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    /**
     * Create a {@link DataFlowRequest} based on incoming request and claims decoded from the access token.
     *
     * @param contextApi Api for accessing request properties.
     * @param claims     Claims token decoded from incoming access token.
     * @return DataFlowRequest
     */
    public DataFlowRequest from(ContainerRequestContextApi contextApi, ClaimToken claims) {
        var props = createProps(contextApi);
        var dataAddress = typeManager.readValue((String) claims.getClaims().get(DataPlaneConstants.DATA_ADDRESS), DataAddress.class);
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(dataAddress)
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(OutputStreamDataSinkFactory.TYPE)
                        .build())
                .trackable(false)
                .id(UUID.randomUUID().toString())
                .properties(props)
                .build();
    }

    /**
     * Put all properties of the incoming request (method, request body, query params...) into a map.
     */
    private static Map<String, String> createProps(ContainerRequestContextApi contextApi) {
        var props = new HashMap<String, String>();
        props.put(METHOD, contextApi.method());
        props.put(QUERY_PARAMS, contextApi.queryParams());
        props.put(PATH, contextApi.path());
        Optional.ofNullable(contextApi.mediaType())
                .ifPresent(mediaType -> {
                    props.put(MEDIA_TYPE, mediaType);
                    props.put(BODY, contextApi.body());
                });
        return props;
    }
}
