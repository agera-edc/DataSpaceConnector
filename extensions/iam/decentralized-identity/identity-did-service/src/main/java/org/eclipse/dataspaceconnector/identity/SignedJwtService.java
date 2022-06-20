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

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.eclipse.dataspaceconnector.iam.did.crypto.CryptoException;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

import java.text.ParseException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Service to generate, verify and deserialize verifiable credentials, which are, in fact, Signed JSON Web Tokens (JWTs).
 */
class SignedJwtService {

    // RFC 7519 Registered (standard) claims
    private static final String ISSUER_CLAIM = "iss";
    private static final String SUBJECT_CLAIM = "sub";
    private static final String AUDIENCE_CLAIM = "aud";
    private static final String EXPIRATION_TIME_CLAIM = "exp";

    // Custom claims
    public static final String OWNER_CLAIM = "owner";
    public static final String VERIFIABLE_CREDENTIAL = "verifiable-credential";

    private final String didUrl;
    private final String connectorName;
    private final PrivateKeyWrapper privateKey;
    private final Clock clock;

    /**
     * Creates a new instance of {@link SignedJwtService}.
     * <p>
     * Although all private key types are possible, in the context of Distributed Identity
     * using an Elliptic Curve key ({@code P-256}) is advisable.
     *
     * @param didUrl        the DID URL to be used as issuer claim.
     * @param connectorName the connector name to be used as owner claim (custom claim).
     * @param privateKey    crypto key used to sign JWTs.
     */
    SignedJwtService(String didUrl, String connectorName, PrivateKeyWrapper privateKey, Clock clock) {
        this.didUrl = didUrl;
        this.connectorName = connectorName;
        this.privateKey = privateKey;
        this.clock = clock;
    }

    /**
     * Creates a signed JWT {@link SignedJWT}.
     *
     * @param audience audience claim value to use in JWT
     * @return a {@code SignedJWT} that is signed with the private key
     */
    public SignedJWT create(String audience) {
        var claimsSet = new JWTClaimsSet.Builder()
                .claim(OWNER_CLAIM, connectorName)
                .issuer(didUrl)
                .subject(VERIFIABLE_CREDENTIAL)
                .audience(audience)
                .expirationTime(Date.from(clock.instant().plus(10, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        var signer = privateKey.signer();
        //prefer ES256 if available, otherwise use the "next best"
        var algorithm = signer.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256) ?
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
            var verify = jwt.verify(publicKey.verifier());

            // verify claims
            var exactMatchClaims = new JWTClaimsSet.Builder()
                    .audience(audience)
                    .subject(VERIFIABLE_CREDENTIAL)
                    .build();
            var requiredClaims = Set.of(
                    ISSUER_CLAIM,
                    SUBJECT_CLAIM,
                    AUDIENCE_CLAIM,
                    EXPIRATION_TIME_CLAIM);
            var claimsVerifier = new DefaultJWTClaimsVerifier<>(exactMatchClaims, requiredClaims);
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
