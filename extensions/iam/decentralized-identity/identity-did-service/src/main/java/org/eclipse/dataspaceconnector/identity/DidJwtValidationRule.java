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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.eclipse.dataspaceconnector.common.token.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

import static org.eclipse.dataspaceconnector.identity.DecentralizedIdentityService.EXPIRATION_TIME_CLAIM;
import static org.eclipse.dataspaceconnector.identity.DecentralizedIdentityService.ISSUER_CLAIM;
import static org.eclipse.dataspaceconnector.identity.DecentralizedIdentityService.SUBJECT_CLAIM;
import static org.eclipse.dataspaceconnector.identity.DecentralizedIdentityService.VERIFIABLE_CREDENTIAL;

public class DidJwtValidationRule implements TokenValidationRule {
    /**
     * Validates the JWT by checking the audience, nbf, and expiration. Accessible for testing.
     *
     * @param toVerify   The jwt including the claims.
     * @param additional No more additional information needed for this validation, can be null.
     */
    @Override
    public Result<SignedJWT> checkRule(SignedJWT toVerify, @Nullable Map<String, Object> additional) {
        try {
            // verify claims
            var exactMatchClaims = new JWTClaimsSet.Builder()
                    .subject(VERIFIABLE_CREDENTIAL)
                    .build();
            var requiredClaims = Set.of(
                    ISSUER_CLAIM,
                    SUBJECT_CLAIM,
                    EXPIRATION_TIME_CLAIM);
            var claimsVerifier = new DefaultJWTClaimsVerifier<>(exactMatchClaims, requiredClaims);
            try {
                claimsVerifier.verify(toVerify.getJWTClaimsSet());
            } catch (BadJWTException e) {
                // claim verification failed
                return Result.failure(e.getMessage());
            }
        } catch (ParseException e) {
            throw new EdcException(e);
        }
        return Result.success(toVerify);
    }
}
