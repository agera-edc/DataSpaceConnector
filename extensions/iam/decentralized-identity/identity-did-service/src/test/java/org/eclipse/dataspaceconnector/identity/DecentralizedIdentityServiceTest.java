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
 *
 */

package org.eclipse.dataspaceconnector.identity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.key.EcPublicKeyWrapper;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.key.KeyPairFactory;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.common.token.JwtDecoratorRegistryImpl;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationServiceImpl;
import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.common.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationContext;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the {@link DecentralizedIdentityService} with a key algorithm. Currently only one algorithm is implemented.
 * See {@link WithP256} for concrete impl.
 */

abstract class DecentralizedIdentityServiceTest {
    private static final Faker FAKER = new Faker();

    private String didUrl = FAKER.internet().url();
    private String connectorName = FAKER.lorem().word();
    private DecentralizedIdentityService identityService;
    private PrivateKeyWrapper privateKey;
    private PublicKeyWrapper publicKey;
    private TokenGenerationContext context = TokenGenerationContext.Builder.newInstance().scope("Foo").build();

    @Test
    void verifyResolveHubUrl() throws IOException {
        var didJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("dids.json");
        var hubUrlDid = new String(didJson.readAllBytes(), StandardCharsets.UTF_8);
        var url = identityService.getHubUrl(new ObjectMapper().readValue(hubUrlDid, DidDocument.class));
        assertEquals("https://myhub.com", url);
    }

    @Test
    void verifyObtainClientCredentials() throws Exception {
        var result = identityService.obtainClientCredentials(context);

        assertTrue(result.succeeded());

        var jwt = SignedJWT.parse(result.getContent().getToken());
        var verifier = publicKey.verifier();
        assertTrue(jwt.verify(verifier));
    }

    @Test
    void verifyJwtToken() throws Exception {
        var signer = privateKey.signer();

        var expiration = new Date().getTime() + TimeUnit.MINUTES.toMillis(10);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("verifiable-credential")
                .issuer("did:ion:123abc")
                .expirationTime(new Date(expiration))
                .build();

        var jwt = new SignedJWT(new JWSHeader.Builder(getHeaderAlgorithm()).keyID("primary").build(), claimsSet);
        jwt.sign(signer);

        var token = jwt.serialize();

        var result = identityService.verifyJwtToken(TokenRepresentation.Builder.newInstance().token(token).build());

        assertTrue(result.succeeded());
        assertEquals("eu", result.getContent().getClaims().get("region"));
    }

    @BeforeEach
    void setUp() throws Exception {
        var keyPair = getKeyPair();
        privateKey = getPrivateKey(keyPair.toECKey());
        publicKey = getPublicKey(keyPair.toPublicJWK().toECKey());

        var didJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("dids.json");
        var hubUrlDid = new String(didJson.readAllBytes(), StandardCharsets.UTF_8);

        DidResolverRegistry didResolver = new TestResolverRegistry(hubUrlDid, keyPair);

        CredentialsVerifier verifier = (document, url) -> Result.success(Map.of("region", "eu"));
        var rulesRegistry = new TokenValidationRulesRegistryImpl();
        rulesRegistry.addRule(new DidJwtValidationRule());
        var tokenGenerationService = new TokenGenerationServiceImpl(new EcPrivateKeyWrapper(keyPair.toECKey()));
        var tokenValidationService = new TokenValidationServiceImpl(id -> {
            try {
                return Result.success(keyPair.toECKey().toPublicKey());
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        }, rulesRegistry);
        var jwtDecoratorRegistry = new JwtDecoratorRegistryImpl();
        jwtDecoratorRegistry.register(new DidJwtDecorator(didUrl, connectorName, Duration.ofMinutes(10)));
        identityService = new DecentralizedIdentityService(tokenGenerationService, tokenValidationService, didResolver, verifier, new ConsoleMonitor(), jwtDecoratorRegistry);
    }

    @NotNull
    protected abstract JWK getKeyPair();

    @NotNull
    protected abstract JWSAlgorithm getHeaderAlgorithm();

    private PublicKeyWrapper getPublicKey(JWK publicKey) throws JOSEException {
        return new EcPublicKeyWrapper(publicKey.toECKey().toECPublicKey());
    }

    private PrivateKeyWrapper getPrivateKey(JWK privateKey) {
        return new EcPrivateKeyWrapper((ECKey) privateKey);
    }

    public static class WithP256 extends DecentralizedIdentityServiceTest {
        @Override
        protected @NotNull JWK getKeyPair() {
            return KeyPairFactory.generateKeyPairP256();
        }

        @Override
        protected @NotNull JWSAlgorithm getHeaderAlgorithm() {
            return JWSAlgorithm.ES256;
        }
    }

    private static class TestResolverRegistry implements DidResolverRegistry {
        private String hubUrlDid;
        private JWK keyPair;

        TestResolverRegistry(String hubUrlDid, JWK keyPair) {
            this.hubUrlDid = hubUrlDid;
            this.keyPair = keyPair;
        }

        @Override
        public void register(DidResolver resolver) {

        }

        @Override
        public Result<DidDocument> resolve(String didKey) {
            try {
                var did = new ObjectMapper().readValue(hubUrlDid, DidDocument.class);
                ECKey key = (ECKey) keyPair.toPublicJWK();
                did.getVerificationMethod().add(VerificationMethod.Builder.create()
                        .type("JsonWebKey2020")
                        .id("test-key")
                        .publicKeyJwk(new EllipticCurvePublicKey(key.getCurve().getName(), key.getKeyType().toString(), key.getX().toString(), key.getY().toString()))
                        .build());
                return Result.success(did);
            } catch (JsonProcessingException e) {
                throw new AssertionError(e);
            }
        }
    }

}
