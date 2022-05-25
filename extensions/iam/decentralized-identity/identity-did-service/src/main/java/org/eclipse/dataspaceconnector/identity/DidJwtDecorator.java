/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *
 */

package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationContext;

import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.identity.DecentralizedIdentityService.OWNER_CLAIM;
import static org.eclipse.dataspaceconnector.identity.DecentralizedIdentityService.VERIFIABLE_CREDENTIAL;

public class DidJwtDecorator implements JwtDecorator {

    private final String didUrl;
    private final String connectorName;
    private final TemporalAmount expiration;

    public DidJwtDecorator(String didUrl, String connectorName, TemporalAmount expiration) {
        this.didUrl = didUrl;
        this.connectorName = connectorName;
        this.expiration = expiration;
    }

    @Override
    public void decorate(TokenGenerationContext context, JWSHeader.Builder header, JWTClaimsSet.Builder claimsSet) {
        header
                .keyID(didUrl);
        claimsSet
                .claim(OWNER_CLAIM, connectorName)
                .issuer(didUrl)
                .subject(VERIFIABLE_CREDENTIAL)
                .audience(context.getAudience())
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(Date.from(Instant.now().plus(expiration)));
    }
}
