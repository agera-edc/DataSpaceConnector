/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.crypto.credentials;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getResourceFileContentAsString;

class VerifiableCredentialFactoryTest {

    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private EcPrivateKeyWrapper privateKey;
    private EcPublicKeyWrapper publicKey;

    @BeforeEach
    void setup() throws JOSEException {
        this.privateKey = new EcPrivateKeyWrapper((ECKey) getJwk("private_p256.pem"));
        this.publicKey = new EcPublicKeyWrapper((ECKey) getJwk("public_p256.pem"));
    }

    @Test
    void createVerifiableCredential() throws Exception {
        var vc = VerifiableCredentialFactory.create(privateKey, "test-connector", "test-audience", clock);

        assertThat(vc).isNotNull();
        assertThat(vc.getJWTClaimsSet().getIssuer()).isEqualTo("test-connector");
        assertThat(vc.getJWTClaimsSet().getSubject()).isEqualTo("verifiable-credential");
        assertThat(vc.getJWTClaimsSet().getAudience()).containsExactly("test-audience");
        assertThat(vc.getJWTClaimsSet().getJWTID()).satisfies(c -> UUID.fromString(c));
        assertThat(vc.getJWTClaimsSet().getExpirationTime()).isEqualTo(now.plus(10, MINUTES).truncatedTo(SECONDS));
    }

    @Test
    void ensureSerialization() throws Exception {
        var vc = VerifiableCredentialFactory.create(privateKey, "test-connector", "test-audience", clock);

        assertThat(vc).isNotNull();
        String jwtString = vc.serialize();

        //deserialize
        var deserialized = SignedJWT.parse(jwtString);

        assertThat(deserialized.getJWTClaimsSet()).isEqualTo(vc.getJWTClaimsSet());
        assertThat(deserialized.getHeader().getAlgorithm()).isEqualTo(vc.getHeader().getAlgorithm());
        assertThat(deserialized.getPayload().toString()).isEqualTo(vc.getPayload().toString());
    }

    @Test
    void verifyJwt() throws Exception {
        var vc = VerifiableCredentialFactory.create(privateKey, "test-connector", "test-audience", clock);
        String jwtString = vc.serialize();

        //deserialize
        var jwt = SignedJWT.parse(jwtString);

        assertThat(VerifiableCredentialFactory.verify(jwt, publicKey, "test-audience")).isTrue();

    }

    private JWK getJwk(String resourceName) throws JOSEException {
        String privateKeyPem = getResourceFileContentAsString(resourceName);
        return JWK.parseFromPEMEncodedObjects(privateKeyPem);
    }
}
