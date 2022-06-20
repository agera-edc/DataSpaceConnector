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
 *       Microsoft Corporation - audience verification
 *
 */

package org.eclipse.dataspaceconnector.identity;

import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class SignedJwtServiceTest {

    static final Faker FAKER = new Faker();

    String didUrl = FAKER.internet().url();
    String connectorName = FAKER.lorem().word();
    String audience = FAKER.internet().url();
    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    SignedJwtService signedJwtService;

    @BeforeEach
    void setup() throws Exception {
        String privateKeyPem = readFile("private_p256.pem");
        var privateKey = (ECKey) ECKey.parseFromPEMEncodedObjects(privateKeyPem);
        signedJwtService = new SignedJwtService(didUrl, connectorName, new EcPrivateKeyWrapper(privateKey), clock);
    }

    @Test
    void createVerifiableCredential() throws Exception {
        var jwt = signedJwtService.create(audience);

        assertThat(jwt).isNotNull();
        assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo(didUrl);
        assertThat(jwt.getJWTClaimsSet().getClaim("owner")).isEqualTo(connectorName);
        assertThat(jwt.getJWTClaimsSet().getClaim("sub")).isEqualTo("verifiable-credential");
        assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isEqualTo(now.plus(Duration.ofMinutes(10)).truncatedTo(SECONDS));
    }

    @Test
    void verifyJwt() throws Exception {
        var jwt = signedJwtService.create(audience);
        var pubKey = readFile("public_p256.pem");

        assertThat(signedJwtService.verify(jwt, new EcPublicKeyWrapper((ECKey) ECKey.parseFromPEMEncodedObjects(pubKey)), audience)).isTrue();
    }

    public String readFile(String filename) throws IOException {
        return new String(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)).readAllBytes());
    }
}
