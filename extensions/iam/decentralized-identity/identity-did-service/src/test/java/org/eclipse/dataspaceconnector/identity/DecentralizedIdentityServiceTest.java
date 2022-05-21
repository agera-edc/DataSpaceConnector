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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the {@link DecentralizedIdentityService} with a key algorithm.
 * See {@link WithP256} for concrete impl.
 */

abstract class DecentralizedIdentityServiceTest {
    private static final Faker FAKER = new Faker();

    String didUrl = FAKER.internet().url();
    String connectorName = FAKER.lorem().word();
    private DecentralizedIdentityService identityService;

    @Test
    void verifyResolveHubUrl() throws IOException {
        var didJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("dids.json");
        var hubUrlDid = new String(didJson.readAllBytes(), StandardCharsets.UTF_8);
        var url = identityService.getHubUrl(new ObjectMapper().readValue(hubUrlDid, DidDocument.class));
        assertEquals("https://myhub.com", url);
    }

    @Test
    void generateAndVerifyJwtToken_valid() {
        var result = identityService.obtainClientCredentials("Foo", "Bar");
        assertTrue(result.succeeded());

        Result<ClaimToken> verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertTrue(verificationResult.succeeded());
        assertEquals("eu", verificationResult.getContent().getClaims().get("region"));
    }

    @Test
    void generateAndVerifyJwtToken_wrongAudience() {
        var result = identityService.obtainClientCredentials("Foo", "Bar");
        assertTrue(result.succeeded());

        Result<ClaimToken> verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar2");
        assertTrue(verificationResult.failed());
    }

    @BeforeEach
    void setUp() throws Exception {
        var keyPair = getKeyPair();
        var privateKey = new EcPrivateKeyWrapper(keyPair.toECKey());

        var didJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("dids.json");
        var hubUrlDid = new String(didJson.readAllBytes(), StandardCharsets.UTF_8);
        var didResolver = new TestResolverRegistry(hubUrlDid, keyPair);
        CredentialsVerifier verifier = (document, url) -> Result.success(Map.of("region", "eu"));
        var signedJwtService = new SignedJwtService(didUrl, connectorName, privateKey);
        identityService = new DecentralizedIdentityService(signedJwtService, didResolver, verifier, new ConsoleMonitor());
    }

    @NotNull
    protected abstract JWK getKeyPair();

    @NotNull
    protected abstract JWSAlgorithm getHeaderAlgorithm();

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
