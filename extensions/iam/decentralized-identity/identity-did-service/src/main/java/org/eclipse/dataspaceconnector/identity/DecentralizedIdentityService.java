/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.identity;

import org.eclipse.dataspaceconnector.common.token.JwtDecorator;
import org.eclipse.dataspaceconnector.common.token.JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyConverter;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationContext;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DecentralizedIdentityService implements IdentityService {
    // RFC 7519 Registered (standard) claims
    static final String ISSUER_CLAIM = "iss";
    static final String SUBJECT_CLAIM = "sub";
    static final String EXPIRATION_TIME_CLAIM = "exp";

    // Custom claims
    static final String OWNER_CLAIM = "owner";
    static final String VERIFIABLE_CREDENTIAL = "verifiable-credential";

    private final TokenGenerationService tokenGenerationService;
    private final TokenValidationService tokenValidationService;

    private final DidResolverRegistry resolverRegistry;
    private final CredentialsVerifier credentialsVerifier;
    private final Monitor monitor;
    private final JwtDecoratorRegistry jwtDecoratorRegistry;

    public DecentralizedIdentityService(
            TokenGenerationService tokenGenerationService,
            TokenValidationService tokenValidationService,
            DidResolverRegistry resolverRegistry,
            CredentialsVerifier credentialsVerifier,
            Monitor monitor,
            JwtDecoratorRegistry jwtDecoratorRegistry) {
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidationService = tokenValidationService;
        this.resolverRegistry = resolverRegistry;
        this.credentialsVerifier = credentialsVerifier;
        this.monitor = monitor;
        this.jwtDecoratorRegistry = jwtDecoratorRegistry;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenGenerationContext context) {
        return tokenGenerationService.generate(context, jwtDecoratorRegistry.getAll().toArray(new JwtDecorator[0]));
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation) {
        monitor.debug("Starting verification...");
        var validationResult = tokenValidationService.validate(tokenRepresentation);
        if (validationResult.failed()) {
            return Result.failure("Token could not be verified!");
        }
        var claims = validationResult.getContent().getClaims();
        var issuer = claims.get(ISSUER_CLAIM);

        monitor.debug("Resolving other party's DID Document");
        var didResult = resolverRegistry.resolve(issuer);
        if (didResult.failed()) {
            return Result.failure("Unable to resolve DID: " + String.join(", ", didResult.getFailureMessages()));
        }
        monitor.debug("Extracting public key");

        // this will return the _first_ public key entry
        Optional<VerificationMethod> publicKey = getPublicKey(didResult.getContent());
        if (publicKey.isEmpty()) {
            return Result.failure("Public Key not found in DID Document!");
        }

        //convert the POJO into a usable PK-wrapper:
        var publicKeyJwk = publicKey.get().getPublicKeyJwk();
        var jwk = KeyConverter.toPublicKey(publicKeyJwk, publicKey.get().getId());
        if (jwk.failed()) {
            return Result.failure("Could not parse key");
        }

        monitor.debug("Verifying JWT with public key...");
        monitor.debug("verification successful! Fetching data from IdentityHub");
        String hubUrl = getHubUrl(didResult.getContent());
        var credentialsResult = credentialsVerifier.verifyCredentials(hubUrl, jwk.getContent());

        monitor.debug("Building ClaimToken");
        var tokenBuilder = ClaimToken.Builder.newInstance();
        var claimToken = tokenBuilder.claims(credentialsResult.getContent()).build();

        return Result.success(claimToken);
    }

    String getHubUrl(DidDocument did) {
        return did.getService().stream().filter(service -> service.getType().equals(DidConstants.HUB_URL)).map(Service::getServiceEndpoint).findFirst().orElseThrow();
    }

    @NotNull
    private Optional<VerificationMethod> getPublicKey(DidDocument did) {
        return did.getVerificationMethod().stream().filter(vm -> DidConstants.ALLOWED_VERIFICATION_TYPES.contains(vm.getType())).findFirst();
    }
}
