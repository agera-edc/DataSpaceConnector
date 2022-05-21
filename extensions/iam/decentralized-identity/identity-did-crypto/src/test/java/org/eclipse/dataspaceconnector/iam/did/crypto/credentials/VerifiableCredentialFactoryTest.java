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

package org.eclipse.dataspaceconnector.iam.did.crypto.credentials;

import com.github.javafaker.Faker;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.helper.TestHelper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class VerifiableCredentialFactoryTest {

    private static final Faker FAKER = new Faker();

    String didUrl = FAKER.internet().url();
    String connectorName = FAKER.lorem().word();
    String audience = FAKER.internet().url();
    JwtFactory jwtFactory;

    @BeforeEach
    void setup() throws JOSEException {
        String privateKeyPem = TestHelper.readFile("private_p256.pem");
        var privateKey = (ECKey) ECKey.parseFromPEMEncodedObjects(privateKeyPem);
        jwtFactory = new JwtFactory(didUrl, connectorName, privateKey);
    }

    @Test
    void createVerifiableCredential() throws ParseException {
        var vc = jwtFactory.create(audience);

        assertThat(vc).isNotNull();
        assertThat(vc.getJWTClaimsSet().getClaim("iss")).isEqualTo(didUrl);
        assertThat(vc.getJWTClaimsSet().getClaim("owner")).isEqualTo(connectorName);
        assertThat(vc.getJWTClaimsSet().getClaim("sub")).isEqualTo("verifiable-credential");
        assertThat(vc.getJWTClaimsSet().getExpirationTime()).isNotNull()
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(11, ChronoUnit.MINUTES));
    }

    @Test
    void verifyJwt() throws Exception {
        var vc = jwtFactory.create(audience);
        String jwtString = vc.serialize();

        //deserialize
        var jwt = SignedJWT.parse(jwtString);
        var pubKey = TestHelper.readFile("public_p256.pem");

        assertThat(jwtFactory.verify(jwt, new EcPublicKeyWrapper((ECKey) ECKey.parseFromPEMEncodedObjects(pubKey)), audience)).isTrue();
    }
}
