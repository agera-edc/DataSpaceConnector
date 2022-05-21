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

package org.eclipse.dataspaceconnector.iam.did.crypto.credentials;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.eclipse.dataspaceconnector.iam.did.crypto.CryptoException;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Convenience/helper class to generate, verify and deserialize verifiable credentials, which are, in fact, Signed JSON Web Tokens (JWTs).
 */
public class JwtFactory {

    public static final String OWNER_CLAIM = "owner";
    private final String didUrl;
    private final String connectorName;
    private final ECKey privateKey;

    public JwtFactory(String didUrl, String connectorName, ECKey privateKey) {
        this.didUrl = didUrl;
        this.connectorName = connectorName;
        this.privateKey = privateKey;
    }

    /**
     * Creates a signed JWT {@link SignedJWT} that contains a set of claims and an issuer. Although all private key types are possible, in the context of Distributed Identity
     * and ION using an Elliptic Curve key ({@code P-256}) is advisable.
     *
     * @return a {@code SignedJWT} that is signed with the private key and contains all claims listed
     */
    public SignedJWT create(String audience) {
        var claimsSetBuilder = new JWTClaimsSet.Builder();

        Map.of(OWNER_CLAIM, connectorName).forEach(claimsSetBuilder::claim);
        var claimsSet = claimsSetBuilder.issuer(didUrl)
                .subject("verifiable-credential")
                .audience(audience)
                .expirationTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSSigner signer = ((PrivateKeyWrapper) new EcPrivateKeyWrapper(privateKey)).signer();
        //prefer ES256 if available, otherwise use the "next best"
        JWSAlgorithm algorithm = signer.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256) ?
                JWSAlgorithm.ES256 :
                signer.supportedJWSAlgorithms().stream().min(Comparator.comparing(Algorithm::getRequirement))
                        .orElseThrow(() -> new CryptoException("No recommended JWS Algorithms for Private Key Signer " + signer.getClass()));
        var header = new JWSHeader(algorithm);

        var vc = new SignedJWT(header, claimsSet);
        try {
            vc.sign(signer);
            return vc;
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Verifies a VerifiableCredential using the issuer's public key
     *
     * @param jwt       a {@link SignedJWT} that was sent by the claiming party.
     * @param publicKey The claiming party's public key, passed as a {@link PublicKeyWrapper}
     * @return true if verified, false otherwise
     */
    public boolean verify(SignedJWT jwt, PublicKeyWrapper publicKey, String audience) {
        try {
            // verify JWT signature
            boolean verify = jwt.verify(publicKey.verifier());

            // verify claims
            DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(
                    new JWTClaimsSet.Builder().audience(audience).build(),
                    new HashSet<>(Arrays.asList("sub", "aud", "exp")));
            try {
                claimsVerifier.verify(jwt.getJWTClaimsSet());
            } catch (BadJWTException e) {
                // claim verification failed
                verify = false;
            }

            return verify;
        } catch (JOSEException | ParseException e) {
            throw new CryptoException(e);
        }
    }
}
