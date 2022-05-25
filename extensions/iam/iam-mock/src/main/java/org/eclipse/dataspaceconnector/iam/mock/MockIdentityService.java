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
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.dataspaceconnector.iam.mock;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationContext;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.time.Instant;

public class MockIdentityService implements IdentityService {
    private final String region;
    private final TypeManager typeManager;

    public MockIdentityService(String region, TypeManager typeManager) {
        this.region = region;
        this.typeManager = typeManager;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenGenerationContext context) {
        var token = new MockToken();
        token.setRegion(region);
        TokenRepresentation tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .expiresIn(Instant.now().plusSeconds(100_000).toEpochMilli())
                .build();
        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation) {
        var token = typeManager.readValue(tokenRepresentation.getToken(), MockToken.class);
        return Result.success(ClaimToken.Builder.newInstance().claim("region", token.region).build());
    }

    private static class MockToken {
        private String region;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
