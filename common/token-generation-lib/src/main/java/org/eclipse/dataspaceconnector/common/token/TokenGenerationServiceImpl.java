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

package org.eclipse.dataspaceconnector.common.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationContext;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class TokenGenerationServiceImpl implements TokenGenerationService {

    private final JWSSigner tokenSigner;
    private final JWSAlgorithm jwsAlgorithm;

    public TokenGenerationServiceImpl(PrivateKeyWrapper privateKey) {
        Objects.requireNonNull(privateKey, "Private key must not be null");
        this.tokenSigner = privateKey.signer();
        if (tokenSigner instanceof ECDSASigner) {
            jwsAlgorithm = JWSAlgorithm.ES256;
        } else {
            jwsAlgorithm = JWSAlgorithm.RS256;
        }
    }

    @Override
    public Result<TokenRepresentation> generate(TokenGenerationContext context, @NotNull JwtDecorator... decorators) {
        var headerBuilder = new JWSHeader.Builder(jwsAlgorithm);
        var claimsBuilder = new JWTClaimsSet.Builder();
        Arrays.stream(decorators).forEach(decorator -> decorator.decorate(context, headerBuilder, claimsBuilder));
        var claims = claimsBuilder.build();

        var token = new SignedJWT(headerBuilder.build(), claims);
        try {
            token.sign(tokenSigner);
        } catch (JOSEException e) {
            return Result.failure("Failed to sign token");
        }
        return Result.success(createTokenRepresentation(token.serialize(), claims));
    }

    private static TokenRepresentation createTokenRepresentation(String token, JWTClaimsSet claimsSet) {
        var builder = TokenRepresentation.Builder.newInstance().token(token);
        if (claimsSet.getExpirationTime() != null) {
            builder.expiresIn(claimsSet.getExpirationTime().getTime());
        }
        return builder.build();
    }
}
